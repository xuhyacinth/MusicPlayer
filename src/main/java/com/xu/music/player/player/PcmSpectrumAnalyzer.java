package com.xu.music.player.player;

import org.jtransforms.fft.DoubleFFT_1D;

import javax.sound.sampled.AudioFormat;
import java.util.Arrays;

/**
 * 将小端有符号 PCM 转为单声道频谱样本，不改写播放数据。
 */
public final class PcmSpectrumAnalyzer {

    private final double[] samples;
    private final DoubleFFT_1D fft;
    private volatile double[] spectrum;
    private int writeIndex;
    private int count;

    public PcmSpectrumAnalyzer(int fftSize) {
        if (fftSize < 2 || Integer.bitCount(fftSize) != 1) {
            throw new IllegalArgumentException("FFT 长度必须是大于 1 的 2 次幂");
        }
        samples = new double[fftSize];
        fft = new DoubleFFT_1D(fftSize);
        spectrum = new double[fftSize / 2];
    }

    public synchronized void accept(byte[] buffer, int offset, int length, AudioFormat format) {
        if (format.isBigEndian() || !AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())) {
            throw new IllegalArgumentException("仅支持小端有符号 PCM");
        }

        var frameSize = format.getFrameSize();
        var channels = format.getChannels();
        var end = offset + length - length % frameSize;
        for (var frameOffset = offset; frameOffset < end; frameOffset += frameSize) {
            samples[writeIndex] = decodeFrame(buffer, frameOffset, channels, format.getSampleSizeInBits());
            writeIndex = (writeIndex + 1) % samples.length;
            if (count < samples.length) {
                count++;
            }
        }
    }

    double decodeFrame(byte[] buffer, int offset, int channels, int sampleSizeInBits) {
        if (channels <= 0) {
            throw new IllegalArgumentException("PCM 参数不受支持");
        }

        int bytes = PcmSamples.bytesPerSample(sampleSizeInBits);
        double scale = Math.scalb(1.0, sampleSizeInBits - 1);
        var mixed = 0.0;
        for (var channel = 0; channel < channels; channel++) {
            var sampleOffset = offset + channel * bytes;
            var sample = PcmSamples.read(buffer, sampleOffset, bytes);
            mixed += sample / scale;
        }
        return mixed / channels;
    }

    public void updateSpectrum() {
        double[] input;
        synchronized (this) {
            if (count < samples.length) {
                return;
            }
            input = new double[samples.length];
            for (var i = 0; i < samples.length; i++) {
                input[i] = samples[(writeIndex + i) % samples.length];
            }
        }

        fft.realForward(input);
        var magnitudes = new double[samples.length / 2];
        magnitudes[0] = Math.abs(input[0]);
        for (var bin = 1; bin < magnitudes.length; bin++) {
            magnitudes[bin] = Math.hypot(input[bin * 2], input[bin * 2 + 1]);
        }
        spectrum = magnitudes;
    }

    public double[] spectrumSnapshot() {
        return spectrum.clone();
    }

    public synchronized void reset() {
        Arrays.fill(samples, 0.0);
        spectrum = new double[samples.length / 2];
        writeIndex = 0;
        count = 0;
    }
}
