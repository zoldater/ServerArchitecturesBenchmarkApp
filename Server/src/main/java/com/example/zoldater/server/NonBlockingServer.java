package com.example.zoldater.server;

import com.example.zoldater.core.BenchmarkBox;
import com.example.zoldater.core.Utils;
import com.example.zoldater.core.enums.PortConstantEnum;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static com.example.zoldater.core.Utils.processSortingMessage;

public class NonBlockingServer extends AbstractServer {
    private final ExecutorService sendingService = Executors.newSingleThreadExecutor();
    private final ExecutorService sortingService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private final Semaphore writingSemaphore = new Semaphore(1);

    protected NonBlockingServer(List<BenchmarkBox> benchmarkBoxes, Semaphore semaphoreSending) {
        super(benchmarkBoxes, semaphoreSending);
    }


    @Override
    public void run() {
        try {
            serverSocketChannel = ServerSocketChannel.open();

            serverSocketChannel.socket().bind(new InetSocketAddress(PortConstantEnum.SERVER_PROCESSING_PORT.getPort()));
            serverSocketChannel.configureBlocking(false);
            selector = Selector.open();
            writingSemaphore.acquire();
            semaphoreSending.release();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                Set<SelectionKey> keys;
                try {
                    selector.select();
                    keys = selector.selectedKeys();
                } catch (ClosedSelectorException e) {
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                BenchmarkBox benchmarkBox = BenchmarkBox.create();
                benchmarkBoxes.add(benchmarkBox);
                benchmarkBox.startClientSession();
                benchmarkBox.startProcessing();
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();
                    if (selectionKey.isAcceptable()) {
                        benchmarkBox.startProcessing();
                        accept(selectionKey);
                    } else {
                        if (selectionKey.isReadable()) {
                            read(selectionKey, benchmarkBox);
                        }

                        if (selectionKey.isValid() && selectionKey.isWritable()) {
                            write(selectionKey, benchmarkBox);
                        }
                    }
                    benchmarkBox.finishProcessing();
                    keyIterator.remove();
                }

                benchmarkBox.finishClientSession();
            }
        } catch (IOException | InterruptedException e) {
            Logger.error(e);
            throw new RuntimeException("Exception during non blocking server work", e);
        } finally {
            close();
        }
    }


    private void accept(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector,
                SelectionKey.OP_READ,
                null);
    }


    private void read(SelectionKey selectionKey, BenchmarkBox benchmarkBox) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        if (selectionKey.attachment() == null) {
            benchmarkBox.startProcessing();
            selectionKey.attach(
                    new SizeReadingAttachment());
        }
        AbstractAttachment attachment = (AbstractAttachment) selectionKey.attachment();

        if (attachment instanceof SizeReadingAttachment) {
            SizeReadingAttachment sizeReadingAttachment = (SizeReadingAttachment) attachment;
            int read = channel.read(sizeReadingAttachment.sizeBuffer);
            if (read == -1) {
                selectionKey.cancel();
                channel.close();
            }
            while (sizeReadingAttachment.sizeBuffer.hasRemaining()) {
            }
            selectionKey.attach(
                    new MessageReadingAttachment(sizeReadingAttachment));

        } else if (attachment instanceof MessageReadingAttachment) {
            MessageReadingAttachment messageReadingAttachment = (MessageReadingAttachment) attachment;
            channel.read(messageReadingAttachment.messageBuffer);
            while (messageReadingAttachment.messageBuffer.hasRemaining()) {
            }
            SortingMessage arrayMessage = SortingMessage.parseFrom(messageReadingAttachment.messageBuffer.array());

            Future<ByteBuffer> futureResponse = sendingService.submit(() -> {
                benchmarkBox.startSorting();
                SortingMessage resultMessage = processSortingMessage(arrayMessage);
                benchmarkBox.finishSorting();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                Utils.writeToStream(resultMessage, byteArrayOutputStream);
                writingSemaphore.release();
                return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
            });

            channel.register(selector,
                    SelectionKey.OP_WRITE,
                    new ProcessingAttachment(messageReadingAttachment, futureResponse));

        }
    }

    private void write(SelectionKey selectionKey, BenchmarkBox benchmarkBox) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        if (selectionKey.attachment() == null) {
            return;
        }
        AbstractAttachment attachment = (AbstractAttachment) selectionKey.attachment();

        if (attachment instanceof ProcessingAttachment) {
            ProcessingAttachment processingAttachment = (ProcessingAttachment) attachment;
            while (!processingAttachment.futureResponse.isDone()) {
            }
            try {
                writingSemaphore.acquire();
                WriteAttachment writeAttachment = new WriteAttachment(processingAttachment, processingAttachment.futureResponse.get());
                channel.write(writeAttachment.messageBuffer);
                if (writeAttachment.messageBuffer.hasRemaining()) {
                    selectionKey.attach(new WriteAttachment(processingAttachment, writeAttachment.messageBuffer));
                } else {
                    selectionKey.attach(null);
                    channel.register(selector,
                            SelectionKey.OP_READ,
                            null);
                }
            } catch (InterruptedException | ExecutionException e) {

                Logger.info(e);
                throw new RuntimeException("Exception while processing message.", e);
            } finally {
                channel.close();
            }

        } else if (attachment instanceof WriteAttachment) {
            WriteAttachment writeAttachment = (WriteAttachment) attachment;
            channel.write(writeAttachment.messageBuffer);
            while (writeAttachment.messageBuffer.hasRemaining()) {
            }

            selectionKey.attach(null);
            channel.register(selector,
                    SelectionKey.OP_READ,
                    null);
            benchmarkBox.finishProcessing();

        }
    }

    @Override
    public void shutdown() {
        sortingService.shutdown();
        sendingService.shutdown();
        try {
            while (!sortingService.awaitTermination(1, TimeUnit.MINUTES)) {
            }
            while (!sendingService.awaitTermination(1, TimeUnit.MINUTES)) {
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        close();
    }


    private abstract class AbstractAttachment {

        AbstractAttachment() {
        }
    }

    private final class SizeReadingAttachment extends AbstractAttachment {
        private final ByteBuffer sizeBuffer;

        SizeReadingAttachment() {
            sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
        }
    }

    private final class MessageReadingAttachment extends AbstractAttachment {
        private final ByteBuffer messageBuffer;

        MessageReadingAttachment(SizeReadingAttachment sizeReadingAttachment) {
            sizeReadingAttachment.sizeBuffer.flip();
            messageBuffer = ByteBuffer.allocate(sizeReadingAttachment.sizeBuffer.getInt());
        }
    }

    private final class ProcessingAttachment extends AbstractAttachment {
        private final Future<ByteBuffer> futureResponse;

        ProcessingAttachment(MessageReadingAttachment messageReadingAttachment,
                             Future<ByteBuffer> futureResponse) {
            this.futureResponse = futureResponse;
        }
    }

    private final class WriteAttachment extends AbstractAttachment {
        private final ByteBuffer messageBuffer;

        WriteAttachment(ProcessingAttachment processingAttachment, ByteBuffer messageBuffer) {
            this.messageBuffer = messageBuffer;
        }

    }

    private void close() {
        if (serverSocketChannel != null) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                throw new RuntimeException("Exception during close server socket channel", e);
            }
        }

        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                throw new RuntimeException("Exception during close selector", e);
            }
        }
    }


}
