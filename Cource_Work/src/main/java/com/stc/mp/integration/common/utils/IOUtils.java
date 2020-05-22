package com.stc.mp.integration.common.utils;

public class IOUtils {

    public static short[] zeros(int size) {
        return new short[size];
    }

    public static long sum(short[] data, int from) {
        long sum = 0;
        for (int i = from; i < data.length - 5; i++) {
            sum += data[i];
        }
        return sum;
    }

    public static long sum(short[] data) {
        long sum = 0;
        for (short datum : data) {
            sum += datum;
        }
        return sum;
    }

    public static short[] abs(short[] data, double k) {
        short[] resultData = new short[data.length];
        for (int i = 0; i < data.length; i++) {
            resultData[i] = (short) Math.abs(data[i] * k);
        }
        return resultData;
    }

    public static long abssum(short[] data, double k) {
        long sum = 0;
        for (short datum : data) {
            sum += (short) Math.abs(datum * k);
        }
        return sum;
    }

    public static long countzeros(short[] data) {
        long counter = 0;
        for (short d : data) {
            if (d == 0) {
                counter++;
            }
        }
        return counter;
    }

    public static float avgabssum(short[] data) {
        long value = abssum(data, 1);
        long n = countzeros(data);

        if (data.length == n) {
            return 0;
        }

        return (float) value / (data.length - n);
    }

    public static long roundEven(float d, int base) {
        return Math.round(d / base) * base;
    }
}
