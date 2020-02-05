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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public class NonBlockingServer extends AbstractServer {
    private final ExecutorService sendingService = Executors.newSingleThreadExecutor();
    private final ExecutorService sortingService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    protected NonBlockingServer(List<BenchmarkBox> benchmarkBoxes, Semaphore semaphoreSending) {
        super(benchmarkBoxes, semaphoreSending);
    }


    @Override
    public void run() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();

            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(PortConstantEnum.SERVER_PROCESSING_PORT.getPort()));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            semaphoreSending.release();

            while (true) {
                int keysNum = selector.select();
                BenchmarkBox benchmarkBox = BenchmarkBox.create();
                benchmarkBoxes.add(benchmarkBox);
                benchmarkBox.startClientSession();
                if (selector.isOpen() && keysNum > 0) {
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
                }
                benchmarkBox.finishClientSession();
            }
        } catch (IOException e) {
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
            if (!sizeReadingAttachment.sizeBuffer.hasRemaining()) {
                selectionKey.attach(
                        new MessageReadingAttachment(sizeReadingAttachment));
            }
        } else if (attachment instanceof MessageReadingAttachment) {
            MessageReadingAttachment messageReadingAttachment = (MessageReadingAttachment) attachment;
            channel.read(messageReadingAttachment.messageBuffer);
            if (!messageReadingAttachment.messageBuffer.hasRemaining()) {
                SortingMessage arrayMessage = SortingMessage.parseFrom(messageReadingAttachment.messageBuffer.array());

                benchmarkBox.startSorting();
                Future<ByteBuffer> futureResponse = sendingService.submit(() -> {
                    SortingMessage resultMessage = processSortingMessage(arrayMessage);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    Utils.writeToStream(resultMessage, byteArrayOutputStream);
                    return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
                });

                channel.register(selector,
                        SelectionKey.OP_WRITE,
                        new ProcessingAttachment(messageReadingAttachment, futureResponse));
            }
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
            if (processingAttachment.futureResponse.isDone()) {
                try {
                    WriteAttachment writeAttachment = new WriteAttachment(processingAttachment, processingAttachment.futureResponse.get());
                    benchmarkBox.finishSorting();
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
            }
        } else if (attachment instanceof WriteAttachment) {
            WriteAttachment writeAttachment = (WriteAttachment) attachment;
            channel.write(writeAttachment.messageBuffer);
            if (!writeAttachment.messageBuffer.hasRemaining()) {

                selectionKey.attach(null);
                channel.register(selector,
                        SelectionKey.OP_READ,
                        null);
            }
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
