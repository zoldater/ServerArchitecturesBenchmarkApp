package com.example.zoldater.server;

import com.example.zoldater.core.Utils;
import com.example.zoldater.core.configuration.AverageTime;
import com.example.zoldater.core.enums.PortConstantEnum;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NonBlockingServer extends AbstractServer {
    private final ExecutorService sendingService = Executors.newSingleThreadExecutor();
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private final AverageTime processingTime = new AverageTime();
    private final AverageTime sortingTime = new AverageTime();

    protected NonBlockingServer(int requestsPerClient) {
        super(requestsPerClient);
    }

    @Override
    public long getProcessingTimes() {
        return processingTime.getAverageTime();
    }

    @Override
    public long getSortingTimes() {
        return sortingTime.getAverageTime();
    }

    @Override
    public void run() {
        long averageClientTime = System.currentTimeMillis();
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();

            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(PortConstantEnum.SERVER_PROCESSING_PORT.getPort()), Integer.MAX_VALUE);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            for (int i = 0; i < requestsPerClient; i++) {
                Logger.info("NonBlocking request #" + i);
                int keysNum = selector.select();
                if (selector.isOpen() && keysNum > 0) {
                    Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey selectionKey = keyIterator.next();
                        if (selectionKey.isAcceptable()) {
                            accept(selectionKey);
                        } else {
                            if (selectionKey.isReadable()) {
                                read(selectionKey);
                            }

                            if (selectionKey.isValid() && selectionKey.isWritable()) {
                                write(selectionKey);
                            }
                        }
                        keyIterator.remove();
                    }
                }
            }
        } catch (IOException e) {
            Logger.error(e);
            throw new RuntimeException("Exception during non blocking server work", e);
        } finally {
            clientTime = System.currentTimeMillis() - averageClientTime;
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


    private void read(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        if (selectionKey.attachment() == null) {
            selectionKey.attach(
                    new SizeReadingAttachment(System.currentTimeMillis()));
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

                Future<ByteBuffer> futureResponse = sendingService.submit(() -> {
                    long sortingStart = System.currentTimeMillis();
                    SortingMessage resultMessage = handleSortingMessage(arrayMessage);
                    sortingTime.addTime(System.currentTimeMillis() - sortingStart);
                    return ByteBuffer.wrap(Utils.serialize(resultMessage));
                });

                channel.register(selector,
                        SelectionKey.OP_WRITE,
                        new ProcessingAttachment(messageReadingAttachment, futureResponse));
            }
        }
    }

    private void write(SelectionKey selectionKey) throws IOException {
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
                    channel.write(writeAttachment.messageBuffer);
                    if (writeAttachment.messageBuffer.hasRemaining()) {
                        selectionKey.attach(new WriteAttachment(processingAttachment, writeAttachment.messageBuffer));
                    } else {
                        processingTime.addTime(System.currentTimeMillis() - attachment.startRequestProcessingTime);
                        selectionKey.attach(null);
                        channel.register(selector,
                                SelectionKey.OP_READ,
                                null);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    channel.close();
                    throw new RuntimeException("Exception while processing message.", e);
                }
            }
        } else if (attachment instanceof WriteAttachment) {
            WriteAttachment writeAttachment = (WriteAttachment) attachment;
            channel.write(writeAttachment.messageBuffer);
            if (!writeAttachment.messageBuffer.hasRemaining()) {
                processingTime.addTime(System.currentTimeMillis() - writeAttachment.startRequestProcessingTime);
                selectionKey.attach(null);
                channel.register(selector,
                        SelectionKey.OP_READ,
                        null);
            }
        }
    }


    private abstract class AbstractAttachment {
        final long startRequestProcessingTime;

        AbstractAttachment(long startRequestProcessingTime) {
            this.startRequestProcessingTime = startRequestProcessingTime;
        }
    }

    private final class SizeReadingAttachment extends AbstractAttachment {
        private final ByteBuffer sizeBuffer;

        SizeReadingAttachment(long startRequestProcessingTime) {
            super(startRequestProcessingTime);
            sizeBuffer = ByteBuffer.allocate(Integer.BYTES);
        }
    }

    private final class MessageReadingAttachment extends AbstractAttachment {
        private final ByteBuffer messageBuffer;

        MessageReadingAttachment(SizeReadingAttachment sizeReadingAttachment) {
            super(sizeReadingAttachment.startRequestProcessingTime);
            sizeReadingAttachment.sizeBuffer.flip();
            messageBuffer = ByteBuffer.allocate(sizeReadingAttachment.sizeBuffer.getInt());
        }
    }

    private final class ProcessingAttachment extends AbstractAttachment {
        private final Future<ByteBuffer> futureResponse;

        ProcessingAttachment(MessageReadingAttachment messageReadingAttachment,
                             Future<ByteBuffer> futureResponse) {
            super(messageReadingAttachment.startRequestProcessingTime);
            this.futureResponse = futureResponse;
        }
    }

    private final class WriteAttachment extends AbstractAttachment {
        private final ByteBuffer messageBuffer;

        WriteAttachment(ProcessingAttachment processingAttachment, ByteBuffer messageBuffer) {
            super(processingAttachment.startRequestProcessingTime);
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
        sendingService.shutdown();
    }


}
