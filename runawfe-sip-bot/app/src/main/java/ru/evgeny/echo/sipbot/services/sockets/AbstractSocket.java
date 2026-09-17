package ru.evgeny.echo.sipbot.services.sockets;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.evgeny.echo.sipbot.utils.CommonUtil;

import java.io.File;
import java.net.ConnectException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public abstract class AbstractSocket {

    public abstract int frameDurationMs();

    public abstract int bufSize();

    public abstract void receiveData(byte[] data);

    public void sendData(byte[] data, boolean skipQ) {
        if (skipQ)
            bytesQ.clear();

        for (byte[] bytes : CommonUtil.split(data, bufSize())) {
            bytesQ.offer(bytes);
        }
    }

    public void stop() {
        if (null != reader)
            reader.interrupt();

        if (null != writer)
            writer.interrupt();
    }

    @Setter
    private volatile Runnable afterDataSentCallback;
    private final ConcurrentLinkedQueue<byte[]> bytesQ = new ConcurrentLinkedQueue<>();
    private Thread reader;
    private Thread writer;

    public boolean isSpeechBufferEmpty() {
        return bytesQ.isEmpty();
    }

    public AbstractSocket(Path sockPath) {
        File file = sockPath.toFile();
        while (!file.exists()) {
            LockSupport.parkNanos(1_000_000);
        }

        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(sockPath);

        final Thread worker = new Thread(() -> {
            // Client side
            boolean tryConnect = true;
            while (tryConnect) {
                tryConnect = false;
                try (SocketChannel clientChannel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
                    clientChannel.connect(address);

                    reader = new Thread(() -> {
                        ByteBuffer buffer = ByteBuffer.allocate(bufSize());
                        while (!Thread.interrupted()) {
                            byte[] bytes = bytesQ.poll();
                            if (null != bytes) {
                                long start = System.currentTimeMillis();
                                writeSocketMessage(buffer, clientChannel, bytes);
                                while (System.currentTimeMillis() - start < frameDurationMs())
                                    LockSupport.parkNanos(1000_000);
                            } else {
                                if (null != afterDataSentCallback) {
                                    afterDataSentCallback.run();
                                    afterDataSentCallback = null;
                                } else {
                                    // Пауза, чтобы не нагружать процессор
                                    LockSupport.parkNanos(1_000_000); // 1 мс
                                }
                            }
                        }
                    });

                    writer = new Thread(() -> {
                        ByteBuffer buffer = ByteBuffer.allocate(bufSize());
                        while (!Thread.interrupted()) {
                            Optional<byte[]> dataO = readSocketMessage(buffer, clientChannel);
                            dataO.ifPresent(this::receiveData);
                        }
                    });

                    reader.start();
                    writer.start();

                    reader.join();
                    writer.join();
                } catch (Exception e) {
                    if (e instanceof ConnectException) {
                        tryConnect = true;
                        LockSupport.parkNanos(100_000_000);
                    } else {
                        log.warn("worker", e);
                    }
                }
            }

            log.info("Finished: {}", address);
        });

        worker.start();
    }


    protected void writeSocketMessage(ByteBuffer buffer, SocketChannel channel, byte[] bytes) {
        buffer.clear();
        buffer.put(bytes);
        buffer.flip();

        while (buffer.hasRemaining()) {
            try {
                channel.write(buffer);
            } catch (Exception e) {
                log.warn("readSocketMessage", e);
                stop();
                break;
            }
        }
    }


    protected Optional<byte[]> readSocketMessage(ByteBuffer buffer, SocketChannel channel) {
        buffer.clear();
        int bytesRead = 0;
        try {
            bytesRead = channel.read(buffer);
        } catch (Exception e) {
            log.warn("readSocketMessage", e);
            if (e instanceof ClosedByInterruptException) {
                log.warn("ClosedByInterruptException", e);
            } else {
                stop();
            }
        }
        if (bytesRead < 0)
            return Optional.empty();

        byte[] bytes = new byte[bytesRead];
        buffer.flip();
        buffer.get(bytes);

        return Optional.of(bytes);
    }
}
