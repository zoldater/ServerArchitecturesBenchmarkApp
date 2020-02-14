package com.example.zoldater.server;

import com.example.zoldater.core.benchmarks.BenchmarkBox;
import com.example.zoldater.core.Utils;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.google.protobuf.InvalidProtocolBufferException;
import ru.spbau.mit.core.proto.SortingProtos.SortingMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

public class NonBlockingServer extends AbstractServer {
    private final ExecutorService sendingService = Executors.newSingleThreadExecutor();
    private final ExecutorService sortingService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private Selector readSelector;
    private Selector writeSelector;
    private ServerSocketChannel serverSocketChannel;
    private final Map<SocketChannel, ByteBuffer> channelToSizeBuffersMap = new ConcurrentHashMap<>();
    private final Map<SocketChannel, ByteBuffer> channelToContentBuffersMap = new ConcurrentHashMap<>();
    private final Map<SocketChannel, BenchmarkBox> channelToBenchmarkBoxMap = new ConcurrentHashMap<>();
    private final Map<SocketChannel, Integer> channelToIterationsMap = new ConcurrentHashMap<>();
    private final Map<SocketChannel, Boolean> channelToProcessingSubmitFlagMap = new ConcurrentHashMap<>();

    protected NonBlockingServer(Semaphore semaphoreSending, CountDownLatch resultsSendingLatch, int clientsCount, int requestsPerClient) {
        super(semaphoreSending, resultsSendingLatch, clientsCount, requestsPerClient);
    }


    @Override
    public void run() {
        try {
            serverSocketChannel = ServerSocketChannel.open();

            serverSocketChannel.socket().bind(new InetSocketAddress(PortConstantEnum.SERVER_PROCESSING_PORT.getPort()), Short.MAX_VALUE);
            serverSocketChannel.configureBlocking(false);
            readSelector = Selector.open();
            writeSelector = Selector.open();
            serverSocketChannel.register(readSelector, SelectionKey.OP_ACCEPT);
            semaphoreSending.release();
            sendingService.submit(() -> {
                while (true) {
                    try {
                        Iterator<SelectionKey> keyIterator;
                        writeSelector.select();
                        keyIterator = writeSelector.selectedKeys().iterator();
                        while (keyIterator.hasNext()) {
                            SelectionKey key = keyIterator.next();
                            if (key.isWritable()) {
                                write(key);
                            }
                            keyIterator.remove();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            while (true) {
                if (Thread.interrupted()) {
                    return;
                }
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
                } catch (ClosedSelectorException e) {
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            benchmarkBoxContainer.add(benchmarkBox);
            benchmarkBox.startClientSession();
            ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
            channelToSizeBuffersMap.put(socketChannel, byteBuffer);
            channelToBenchmarkBoxMap.put(socketChannel, benchmarkBox);
            channelToIterationsMap.put(socketChannel, 1);
            socketChannel.register(readSelector, SelectionKey.OP_READ);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void read(SelectionKey selectionKey) {
        SocketChannel channel = (SocketChannel) selectionKey.channel();

        BenchmarkBox benchmarkBox = channelToBenchmarkBoxMap.get(channel);
        ByteBuffer sizeByteBuffer = channelToSizeBuffersMap.get(channel);

        // Только начали читать sizeBuffer
        if (sizeByteBuffer.position() != 4) {
            try {
                long readBytes = channel.read(sizeByteBuffer);
                if (readBytes == -1) {
                    selectionKey.cancel();
                    benchmarkBox.finishClientSession();
                } else {
                    benchmarkBox.startProcessing();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            if (!channelToContentBuffersMap.containsKey(channel)) {
                sizeByteBuffer.flip();
                int sizeByteBufferInt = sizeByteBuffer.getInt();
                channelToContentBuffersMap.put(channel, ByteBuffer.allocate(sizeByteBufferInt));
            }
            ByteBuffer contentBuffer = channelToContentBuffersMap.get(channel);
            try {

                channel.read(contentBuffer);

                if (contentBuffer.position() == contentBuffer.capacity()
                        && !channelToProcessingSubmitFlagMap.containsKey(channel)) {
                    channelToProcessingSubmitFlagMap.put(channel, true);
                    sortingService.submit(() -> processContent(channel));
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void processContent(SocketChannel channel) {
        ByteBuffer messageContentByteBuffer = channelToContentBuffersMap.get(channel);
        ByteBuffer sizeByteBuffer = channelToSizeBuffersMap.get(channel);
        BenchmarkBox benchmarkBox = channelToBenchmarkBoxMap.get(channel);
        messageContentByteBuffer.flip();
        byte[] messageBytesArray = messageContentByteBuffer.array();
        try {
            SortingMessage sortingMessage = SortingMessage.parseFrom(messageBytesArray);
            benchmarkBox.startSorting();
            SortingMessage sortedMessage = Utils.processSortingMessage(sortingMessage);
            benchmarkBox.finishSorting();
            byte[] sortedMessageBytes = sortedMessage.toByteArray();
            sizeByteBuffer.clear();
            sizeByteBuffer.putInt(sortedMessageBytes.length);
            sizeByteBuffer.flip();
            messageContentByteBuffer.clear();
            messageContentByteBuffer.put(ByteBuffer.wrap(sortedMessageBytes));
            messageContentByteBuffer.flip();
            channelToProcessingSubmitFlagMap.remove(channel);
            writeSelector.wakeup();
            channel.register(writeSelector, SelectionKey.OP_WRITE);
        } catch (InvalidProtocolBufferException | ClosedChannelException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer messageContentByteBuffer = channelToContentBuffersMap.get(socketChannel);
        ByteBuffer sizeByteBuffer = channelToSizeBuffersMap.get(socketChannel);
        BenchmarkBox benchmarkBox = channelToBenchmarkBoxMap.get(socketChannel);
        try {
            socketChannel.write(new ByteBuffer[]{sizeByteBuffer, messageContentByteBuffer});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (sizeByteBuffer.position() == sizeByteBuffer.capacity() &&
                messageContentByteBuffer.position() ==
                        messageContentByteBuffer.capacity()) {
            benchmarkBox.finishProcessing();
            sizeByteBuffer.clear();
            messageContentByteBuffer.clear();
            key.cancel();
            Integer integer = channelToIterationsMap.get(socketChannel);

            channelToIterationsMap.put(socketChannel, integer + 1);
            if (integer == requestsPerClient) {
                benchmarkBox.finishClientSession();
                resultsSendingLatch.countDown();
            }
        }
    }

    @Override
    public void shutdown() {
        try {
            readSelector.close();
            writeSelector.close();
            sortingService.shutdownNow();
            sendingService.shutdownNow();
            for (SocketChannel socketChannel : channelToIterationsMap.keySet()) {
                socketChannel.close();
            }
            channelToIterationsMap.clear();
            channelToSizeBuffersMap.clear();
            channelToContentBuffersMap.clear();

            channelToBenchmarkBoxMap.clear();
            while (!sortingService.awaitTermination(1, TimeUnit.SECONDS)) {
            }
            while (!sendingService.awaitTermination(1, TimeUnit.SECONDS)) {
            }
            serverSocketChannel.close();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
