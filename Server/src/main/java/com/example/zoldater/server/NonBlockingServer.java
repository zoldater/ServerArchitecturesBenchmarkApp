package com.example.zoldater.server;

import com.example.zoldater.core.BenchmarkBox;
import com.example.zoldater.core.Utils;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NonBlockingServer extends AbstractServer {
    private final ExecutorService sendingService = Executors.newSingleThreadExecutor();
    private final ExecutorService sortingService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private Selector readSelector;
    private Selector writeSelector;
    private int connectedClients = 0;
    private long realStartTime;
    private ServerSocketChannel serverSocketChannel;
    private final Map<SocketChannel, MessageAttachment> channelToAttachmentMap = new ConcurrentHashMap<>();

    protected NonBlockingServer(Semaphore semaphoreSending, CountDownLatch resultsSendingLatch, int clientsCount, int requestsPerClient) {
        super(semaphoreSending, resultsSendingLatch, clientsCount, requestsPerClient);
    }


    @Override
    public void run() {
        try {
            serverSocketChannel = ServerSocketChannel.open();

            serverSocketChannel.socket().bind(new InetSocketAddress(PortConstantEnum.SERVER_PROCESSING_PORT.getPort()));
            serverSocketChannel.configureBlocking(false);
            readSelector = Selector.open();
            writeSelector = Selector.open();
            serverSocketChannel.register(readSelector, SelectionKey.OP_ACCEPT);
            semaphoreSending.release();

            while (serverSocketChannel.isOpen()) {
                try {
                    readSelector.select();
                    Iterator<SelectionKey> keyIterator = readSelector.selectedKeys().iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey selectionKey = keyIterator.next();
                        if (selectionKey.isAcceptable()) {
                            accept(selectionKey);
                        } else if (selectionKey.isReadable()) {
                            read(selectionKey);
                        }
                        keyIterator.remove();
                    }
                    writeSelector.selectNow();
                    keyIterator = writeSelector.selectedKeys().iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey key = keyIterator.next();

                        if (key.isWritable()) {
                            write(key);
                        }
                        keyIterator.remove();
                    }
                } catch (ClosedSelectorException e) {
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            Logger.error(e);
            return;
        } finally {
            shutdown();
        }
    }

    private void accept(SelectionKey selectionKey) {
        try {
            SocketChannel socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
            if (socketChannel == null) return;
            socketChannel.configureBlocking(false);
            BenchmarkBox benchmarkBox = BenchmarkBox.create();
            benchmarkBoxes.add(benchmarkBox);
            MessageAttachment attachment = new MessageAttachment(benchmarkBox);
            attachment.startClientSession();
            channelToAttachmentMap.put(socketChannel, attachment);
            countDownLatch.countDown();
            connectedClients++;
            if (connectedClients == clientsCount) {
                realStartTime = System.currentTimeMillis();
            }
            socketChannel.register(readSelector, SelectionKey.OP_READ);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void read(SelectionKey selectionKey) {
        SocketChannel channel = (SocketChannel) selectionKey.channel();

        // Только что пришли из accept, attachment пустой
        final MessageAttachment messageAttachment = channelToAttachmentMap.get(channel);
        if (messageAttachment.messageSizeBuffer.remaining() == 4) {
            try {
                int bytesCount = channel.read(messageAttachment.messageSizeBuffer);
                if (bytesCount < 0) {
                    selectionKey.cancel();
                } else {
                    messageAttachment.startProcessing();
                }
                if (messageAttachment.messageSizeBuffer.position() == 4) {
                    messageAttachment.messageSizeBuffer.flip();
                    messageAttachment.messageContentSize = messageAttachment.messageSizeBuffer.getInt();
                    messageAttachment.messageContent = ByteBuffer.allocate(messageAttachment.messageContentSize);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                channel.read(messageAttachment.messageContent);
            } catch (IOException e) {
                Logger.error(e);
                throw new RuntimeException(e);
            }
            if (messageAttachment.messageContent.position() == messageAttachment.messageContentSize) {
                messageAttachment.messageContent.flip();
                final Semaphore semaphore = new Semaphore(1);
                try {
                    semaphore.acquire();
                    sortingService.submit(() -> {
                        try {
                            byte[] messageBytesArray = messageAttachment.messageContent.array();
                            SortingProtos.SortingMessage sortingMessage = SortingProtos.SortingMessage.parseFrom(messageBytesArray);
                            messageAttachment.startSorting();
                            SortingProtos.SortingMessage sortedMessage = Utils.processSortingMessage(sortingMessage);
                            messageAttachment.finishSorting();
                            byte[] sortedMessageBytes = sortedMessage.toByteArray();
                            messageAttachment.messageSizeBuffer.clear();
                            messageAttachment.messageSizeBuffer.putInt(sortedMessageBytes.length);
                            messageAttachment.messageSizeBuffer.flip();
                            messageAttachment.messageContent = ByteBuffer.wrap(sortedMessageBytes);
                            semaphore.release();
                        } catch (InvalidProtocolBufferException e) {
                            Logger.error(e);
                            throw new RuntimeException(e);
                        }
                    });
                    semaphore.acquire();
                    channel.register(writeSelector, SelectionKey.OP_WRITE);
                } catch (InterruptedException | ClosedChannelException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void write(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        final MessageAttachment messageAttachment = channelToAttachmentMap.get(socketChannel);
        ByteBuffer[] buffers = {
                messageAttachment.messageSizeBuffer,
                messageAttachment.messageContent
        };
        if (messageAttachment.messageSizeBuffer.position() == 4
                && messageAttachment.messageContent.position() == messageAttachment.messageContent.capacity()) {
            messageAttachment.finishProcessing();
            messageAttachment.messageSizeBuffer.clear();
            messageAttachment.messageContent.clear();
            key.cancel();
        } else {
            sendingService.submit(() -> {
                try {
                    socketChannel.write(buffers);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            try {
                socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
                Logger.info("register writeSelector in write");
            } catch (ClosedChannelException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public void shutdown() {
        try {
            readSelector.close();
            sortingService.shutdownNow();
            sendingService.shutdownNow();
            writeSelector.close();
            while (!sortingService.awaitTermination(1, TimeUnit.SECONDS)) {
            }
            while (!sendingService.awaitTermination(1, TimeUnit.SECONDS)) {
            }
            serverSocketChannel.close();
        } catch (IOException | InterruptedException e) {
            Logger.error(e);
            throw new RuntimeException(e);
        }

    }


    private class MessageAttachment {
        // После окончания write проверяем на == requestsPerClient и завершаем сессию и метод,
        // иначе регистрируем readSelector и продолжаем работу
        private AtomicInteger requestCount = new AtomicInteger(0);
        private final BenchmarkBox benchmarkBox;
        private final ByteBuffer messageSizeBuffer = ByteBuffer.allocate(4);
        private ByteBuffer messageContent;
        private int messageContentSize;

        private MessageAttachment(BenchmarkBox benchmarkBox) {
            this.benchmarkBox = benchmarkBox;
        }

        public void startClientSession() {
            benchmarkBox.startClientSession();
        }

        public void finishClientSession() {
            benchmarkBox.finishClientSession();
        }

        public void startProcessing() {
            benchmarkBox.startProcessing();
        }

        public void finishProcessing() {
            benchmarkBox.finishProcessing();
        }

        public void startSorting() {
            benchmarkBox.startSorting();
        }

        public void finishSorting() {
            benchmarkBox.finishSorting();
        }
    }
}
