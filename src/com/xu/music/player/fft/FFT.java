package com.xu.music.player.fft;

import java.util.stream.Stream;

/*************************************************************************
 * Compilation: javac FFT.java Execution: java FFT N Dependencies: Complex.java
 *
 * Compute the FFT and inverse FFT of a length N complex sequence. Bare bones
 * implementation that runs in O(N log N) time. Our goal is to optimize the
 * clarity of the code, rather than performance.
 *
 * Limitations ----------- - assumes N is a power of 2
 *
 * - not the most memory efficient algorithm (because it uses an object type for
 * representing complex numbers and because it re-allocates memory for the
 * subarray, instead of doing in-place or reusing a single temporary array)
 *
 *************************************************************************/
public class FFT {

    /**
     * compute the FFT of x[], assuming its length is a power of 2
     *
     * @param x
     * @return
     */
    public static Complex[] fft(Complex[] x) {
        int n = x.length;

        // base case
        if (n == 1) {
            return new Complex[]{x[0]};
        }

        // radix 2 Cooley-Tukey FFT
        if (n % 2 != 0) {
            throw new RuntimeException("N is not a power of 2");
        }

        // fft of even terms
        Complex[] even = new Complex[n / 2];
        for (int k = 0; k < n / 2; k++) {
            even[k] = x[2 * k];
        }
        Complex[] q = fft(even);

        // fft of odd terms
        Complex[] odd = even; // reuse the array
        for (int k = 0; k < n / 2; k++) {
            odd[k] = x[2 * k + 1];
        }
        Complex[] r = fft(odd);

        // combine
        Complex[] y = new Complex[n];
        for (int k = 0; k < n / 2; k++) {
            double kth = -2 * k * Math.PI / n;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k] = q[k].plus(wk.times(r[k]));
            y[k + n / 2] = q[k].minus(wk.times(r[k]));
        }
        return y;
    }

    /**
     * compute the inverse FFT of x[], assuming its length is a power of 2
     *
     * @param x
     * @return
     */
    public static Complex[] ifft(Complex[] x) {
        int n = x.length;
        Complex[] y = new Complex[n];

        // take conjugate
        for (int i = 0; i < n; i++) {
            y[i] = x[i].conjugate();
        }

        // compute forward FFT
        y = fft(y);

        // take conjugate again
        for (int i = 0; i < n; i++) {
            y[i] = y[i].conjugate();
        }

        // divide by N
        for (int i = 0; i < n; i++) {
            y[i] = y[i].scale(1.0 / n);
        }

        return y;

    }

    /**
     * compute the circular convolution of x and y
     *
     * @param x
     * @param y
     * @return
     */
    public static Complex[] cconvolve(Complex[] x, Complex[] y) {

        // should probably pad x and y with 0s so that they have same length and are powers of 2
        if (x.length != y.length) {
            throw new RuntimeException("Dimensions don't agree");
        }

        int n = x.length;

        // compute FFT of each sequence，求值
        Complex[] a = fft(x);
        Complex[] b = fft(y);

        // point-wise multiply，点值乘法
        Complex[] c = new Complex[n];
        for (int i = 0; i < n; i++) {
            c[i] = a[i].times(b[i]);
        }

        // compute inverse FFT，插值
        return ifft(c);
    }

    /**
     * compute the linear convolution of x and y
     *
     * @param x
     * @param y
     * @return
     */
    public static Complex[] convolve(Complex[] x, Complex[] y) {
        Complex zero = new Complex(0, 0);
        // 2n次数界，高阶系数为0.
        Complex[] a = new Complex[2 * x.length];
        for (int i = 0; i < x.length; i++) {
            a[i] = x[i];
        }
        for (int i = x.length; i < 2 * x.length; i++) {
            a[i] = zero;
        }

        Complex[] b = new Complex[2 * y.length];
        for (int i = 0; i < y.length; i++) {
            b[i] = y[i];
        }
        for (int i = y.length; i < 2 * y.length; i++) {
            b[i] = zero;
        }

        return cconvolve(a, b);
    }

    /**
     * Complex[] to double array for MusicPlayer
     *
     * @param x
     * @return
     */
    public static Double[] array(Complex[] x) {//for MusicPlayer
        int len = x.length;//修正幅过小 输出幅值 * 2 / length * 50
        return Stream.of(x).map(a -> a.abs() * 2 / len * 50).toArray(Double[]::new);
    }

    /**
     * display an array of Complex numbers to standard output
     *
     * @param x
     * @param title
     */
    public static void show(Double[] x, String... title) {
        for (String s : title) {
            System.out.print(s);
        }
        System.out.println();
        System.out.println("-------------------");
        for (int i = 0, len = x.length; i < len; i++) {
            System.out.println(x[i]);
        }
        System.out.println();
    }

    /**
     * display an array of Complex numbers to standard output
     *
     * @param x
     * @param title
     */
    public static void show(Complex[] x, String title) {
        System.out.println(title);
        System.out.println("-------------------");
        for (int i = 0, len = x.length; i < len; i++) {
            // 输出幅值需要 * 2 / length
            System.out.println(x[i].abs() * 2 / len);
        }
        System.out.println();
    }

    /**
     * 将数组数据重组成2的幂次方输出
     *
     * @param data
     * @return
     */
    public static Double[] pow2DoubleArr(Double[] data) {

        // 创建新数组
        Double[] newData = null;

        int dataLength = data.length;

        int sumNum = 2;
        while (sumNum < dataLength) {
            sumNum = sumNum * 2;
        }
        int addLength = sumNum - dataLength;

        if (addLength != 0) {
            newData = new Double[sumNum];
            System.arraycopy(data, 0, newData, 0, dataLength);
            for (int i = dataLength; i < sumNum; i++) {
                newData[i] = 0d;
            }
        } else {
            newData = data;
        }

        return newData;
    }

    /**
     * 去偏移量
     *
     * @param originalArr 原数组
     * @return 目标数组
     */
    public static Double[] deskew(Double[] originalArr) {
        // 过滤不正确的参数
        if (originalArr == null || originalArr.length <= 0) {
            return null;
        }

        // 定义目标数组
        Double[] resArr = new Double[originalArr.length];

        // 求数组总和
        Double sum = 0D;
        for (int i = 0; i < originalArr.length; i++) {
            sum += originalArr[i];
        }

        // 求数组平均值
        Double aver = sum / originalArr.length;

        // 去除偏移值
        for (int i = 0; i < originalArr.length; i++) {
            resArr[i] = originalArr[i] - aver;
        }

        return resArr;
    }

}