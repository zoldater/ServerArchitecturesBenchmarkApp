package com.example.zoldater.server;

import com.example.zoldater.core.BenchmarkBox;
import com.example.zoldater.core.Utils;
import com.example.zoldater.core.enums.PortConstantEnum;
import com.google.protobuf.InvalidProtocolBufferException;
import org.tinylog.Logger;
import ru.spbau.mit.core.proto.SortingProtos;
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.example.zoldater.core.Utils.processSortingMessage;

public class NonBlockingServer extends AbstractServer {
    private final ExecutorService sendingService = Executors.newSingleThreadExecutor();
    private final ExecutorService sortingService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
    private Selector readSelector;
    private Selector writeSelector;
    private ServerSocketChannel serverSocketChannel;
    private final Lock lock = new ReentrantLock();
    private final Condition writeCondition = lock.newCondition();

    protected NonBlockingServer(List<BenchmarkBox> benchmarkBoxes, Semaphore semaphoreSending) {
        super(benchmarkBoxes, semaphoreSending);
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
            sendingService.submit(() -> {
                while (true) {
                    Set<SelectionKey> keys;
                    try {
                        lock.lock();
                        int ready = writeSelector.selectNow();
                        if (ready == 0) {
                            while (writeSelector.keys().isEmpty()) {
                                writeCondition.await();
                            }
                        }
                        writeSelector.select();
                        keys = writeSelector.selectedKeys();
                    } catch (ClosedSelectorException | InterruptedException e) {
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        lock.unlock();
                    }

                    Iterator<SelectionKey> it = keys.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        if (key.isWritable()) {
                            SocketChannel client = (SocketChannel) (key.channel());
                            MessageAttachment attachment =
                                    (MessageAttachment) (key.attachment());
                            ByteBuffer[] buffers = {
                                    attachment.messageSizeBuffer,
                                    attachment.messageContent
                            };
                            try {
                                client.write(buffers);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            if (attachment.messageSizeBuffer.position() == 4 &&
                                    attachment.messageContent.position() ==
                                            attachment.messageContent.capacity()) {
                                attachment.finishProcessing();
                                attachment.messageSizeBuffer.clear();
                                attachment.status = AttachmentStatus.READY_TO_READ;
                                key.cancel();
                            }
                        }
                        it.remove();
                    }
                }


            });
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
        SocketChannel socketChannel = null;
        try {
            socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
            if (socketChannel == null) return;
            socketChannel.configureBlocking(false);
            BenchmarkBox benchmarkBox = BenchmarkBox.create();
            benchmarkBoxes.add(benchmarkBox);
            MessageAttachment attachment = new MessageAttachment(socketChannel, benchmarkBox);
            attachment.startClientSession();
            socketChannel.register(readSelector,
                    SelectionKey.OP_READ,
                    attachment);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void read(SelectionKey selectionKey) {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        MessageAttachment attachment = (MessageAttachment) selectionKey.attachment();

        if (AttachmentStatus.READY_TO_READ.equals(attachment.status)) {
            try {
                int bytesCount = channel.read(attachment.messageSizeBuffer);
                if (bytesCount < 0) {
                    selectionKey.cancel();
                    attachment.finishClientSession();
                } else {
                    attachment.startProcessing();
                }
                if (attachment.messageSizeBuffer.position() == 4) {
                    attachment.messageSizeBuffer.flip();
                    attachment.messageContentSize = attachment.messageSizeBuffer.getInt();
                    attachment.status = AttachmentStatus.READY_TO_PROCESS;
                    attachment.messageContent = ByteBuffer.allocate(attachment.messageContentSize);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                channel.read(attachment.messageContent);
            } catch (IOException e) {
                Logger.error(e);
                throw new RuntimeException(e);
            }
            if (attachment.messageContent.position() == attachment.messageContentSize) {
                attachment.messageContent.flip();
                sortingService.submit(() -> {
                    try {
                        lock.lock();
                        byte[] messageBytesArray = attachment.messageContent.array();
                        SortingProtos.SortingMessage sortingMessage = SortingProtos.SortingMessage.parseFrom(messageBytesArray);
                        attachment.startSorting();
                        SortingProtos.SortingMessage sortedMessage = Utils.processSortingMessage(sortingMessage);
                        attachment.finishSorting();
                        byte[] sortedMessageBytes = sortedMessage.toByteArray();
                        attachment.messageSizeBuffer.clear();
                        attachment.messageSizeBuffer.putInt(sortedMessageBytes.length);
                        attachment.messageSizeBuffer.flip();
                        attachment.messageContent = ByteBuffer.wrap(sortedMessageBytes);
                        attachment.status = AttachmentStatus.READY_TO_WRITE;
                        attachment.socketChannel.register(writeSelector, SelectionKey.OP_WRITE, attachment);
                        writeCondition.signal();
                    } catch (InvalidProtocolBufferException | ClosedChannelException e) {
                        Logger.error(e);
                        throw new RuntimeException(e);
                    } finally {
                        lock.unlock();
                    }
                });
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
        private final SocketChannel socketChannel;
        private final BenchmarkBox benchmarkBox;
        private final ByteBuffer messageSizeBuffer = ByteBuffer.allocate(4);
        private ByteBuffer messageContent;
        private int messageContentSize;

        private AttachmentStatus status = AttachmentStatus.READY_TO_READ;

        private MessageAttachment(SocketChannel socketChannel, BenchmarkBox benchmarkBox) {
            this.socketChannel = socketChannel;
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

    private static enum AttachmentStatus {
        READY_TO_READ(1),
        READY_TO_PROCESS(2),
        READY_TO_WRITE(3);
        private final int code;

        AttachmentStatus(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
