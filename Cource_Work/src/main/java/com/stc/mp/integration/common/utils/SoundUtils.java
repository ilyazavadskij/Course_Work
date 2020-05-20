package com.stc.mp.integration.common.utils;

import com.stc.mp.integration.common.wavheader.WavHeaderReader;
import com.stc.mp.integration.diarize.data.ChannelData;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/*
 * @author: Matvey Timchenko
 * DateTime: 02.10.2019:13:40
 */

public class SoundUtils {


    public static void setWavLength(File file) throws IOException {
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;//TODO
        setWavLength(file, byteOrder);
    }

    private static void setWavLength(File file, ByteOrder byteOrder) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");

        long fileSize = raf.length();
        int soundSize = (int) ((fileSize - WavHeaderReader.HEADER_SIZE));

        raf.seek(4);
        raf.write(ByteBuffer.allocate(4).order(byteOrder).putInt(36 + soundSize).array());
        raf.seek(40);
        raf.write(ByteBuffer.allocate(4).order(byteOrder).putInt(soundSize).array());
        raf.close();
    }


    public static ChannelData stereoBufferToChannelData(byte[] buffer, ByteOrder byteOrder) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.order(byteOrder);
        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();

        int size = shortBuffer.capacity();
        short[] leftChannel = new short[size / 2];
        short[] rightChannel = new short[size / 2];
        for (int i = 0; i < size / 2; i++) {
            leftChannel[i] = shortBuffer.get();
            rightChannel[i] = shortBuffer.get();
        }

        return new ChannelData(leftChannel, rightChannel);
    }

    public static byte[] shortBuffersToByteStereoBuffer(short[] yl, short[] yr) {
        int size = yl.length;
        short[] array = new short[2 * size];
        for (int i = 0; i < size; i++) {
            array[2 * i] = yl[i];
            array[2 * i + 1] = yr[i];
        }
        return shortBufferToByteBuffer(array);
    }

    public static byte[] shortBufferToByteBuffer(short[] array) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2 * array.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);//TODO
        byteBuffer.asShortBuffer().put(array);

        return byteBuffer.array();
    }

    public static double getSoundLevel(ByteBuffer byteBuffer, boolean sizeByte) {
        double sum = 0;
        if (sizeByte) {
            byte[] buff = byteBuffer.array();
            for (int i = 0; i < buff.length; i+=2) {
                sum += Math.abs(buff[i]);
            }
            return Math.log((sum / buff.length));
        } else {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);//TODO
            ShortBuffer buf = byteBuffer.asShortBuffer();
            //TODO need optimization
            while (buf.hasRemaining()) {
                short s = buf.get();
                if (buf.hasRemaining()) { //skip each second value
                    buf.get();
                }
                sum += Math.abs(s);
            }
            return Math.log((sum / buf.capacity()));
        }
    }

    public byte[] intToByteArray(int value) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return bytes;
    }

    public static long abssum(short[] data) {
        long value = 0;
        for (short d : data) {
            value += Math.abs(d);
        }
        return value;
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
        long value = abssum(data);
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
