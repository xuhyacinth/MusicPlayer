package com.xu.music.player.player;

import org.jflac.FLACDecoder;
import org.jflac.FrameDecodeException;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** 保留原位深的 FLAC 解码流，明确处理结尾、取消和异常帧的退出条件。 */
final class FlacPcmInputStream extends InputStream {
    private final AudioInputStream source;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final FLACDecoder decoder;
    private final StreamInfo info;
    private ByteData pcm;
    private int position;
    private boolean eof;

    private FlacPcmInputStream(AudioInputStream source) throws IOException {
        this.source = source;
        var input = new FilterInputStream(source) {
            @Override public int read() throws IOException {
                checkCancelled();
                return in.read();
            }

            @Override public int read(byte[] data, int offset, int length) throws IOException {
                checkCancelled();
                return in.read(data, offset, length);
            }
        };
        decoder = new FLACDecoder(input) {
            private long lastFramePosition = -1;

            @Override public void readFrame() throws IOException, FrameDecodeException {
                checkCancelled();
                // jFLAC 会重试坏帧；没有消费数据时必须退出，不能在同一个帧头上空转。
                long currentPosition = getTotalBytesRead();
                if (currentPosition == lastFramePosition) {
                    throw new IOException("FLAC 帧解析未取得进展，已停止解码");
                }
                lastFramePosition = currentPosition;
                super.readFrame();
            }
        };
        decoder.readMetadata();
        info = decoder.getStreamInfo();
        if (info == null) throw new IOException("FLAC 缺少音频流信息");
    }

    static AudioInputStream decode(AudioInputStream source) throws IOException {
        try {
            var input = new FlacPcmInputStream(source);
            var info = input.info;
            var format = new AudioFormat(info.getSampleRate(), info.getBitsPerSample(),
                    info.getChannels(), true, false);
            long frames = info.getTotalSamples() > 0 ? info.getTotalSamples() : AudioSystem.NOT_SPECIFIED;
            return new AudioInputStream(input, format, frames);
        } catch (IOException | RuntimeException exception) {
            try {
                source.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    @Override public int read() throws IOException {
        if (!ensurePcm()) return -1;
        return pcm.getData()[position++] & 0xff;
    }

    @Override public int read(byte[] data, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, data.length);
        checkCancelled();
        if (length == 0) return 0;
        if (!ensurePcm()) return -1;
        int count = Math.min(length, pcm.getLen() - position);
        System.arraycopy(pcm.getData(), position, data, offset, count);
        position += count;
        return count;
    }

    private boolean ensurePcm() throws IOException {
        checkCancelled();
        if (pcm != null && position < pcm.getLen()) return true;
        if (eof) return false;
        // 到达声明的总采样数后不再解析尾部标签，避开 jFLAC SPI 的结尾重试循环。
        long total = info.getTotalSamples();
        if (total > 0 && decoder.getSamplesDecoded() >= total) {
            eof = true;
            return false;
        }
        var frame = decoder.readNextFrame();
        checkCancelled();
        if (frame == null) {
            eof = true;
            if (total > 0 && decoder.getSamplesDecoded() < total) {
                throw new IOException("FLAC 文件提前结束，采样数不足");
            }
            return false;
        }
        pcm = decoder.decodeFrame(frame, pcm);
        position = 0;
        if (pcm.getLen() == 0) throw new IOException("FLAC 解码未返回有效数据");
        return true;
    }

    private void checkCancelled() throws IOException {
        if (closed.get()) throw new IOException("FLAC 音频流已关闭");
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("FLAC 解码已取消");
        }
    }

    @Override public void close() throws IOException {
        // 不持有解码锁；先标记取消并关闭源，读取和坏帧重试会在检查点退出。
        if (closed.compareAndSet(false, true)) source.close();
    }
}
