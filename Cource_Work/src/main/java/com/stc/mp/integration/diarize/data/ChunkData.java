package com.stc.mp.integration.diarize.data;

import com.stc.mp.integration.common.utils.SoundUtils;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ChunkData implements Closeable {
    private BufferedInputStream bins;

    public ChunkData(BufferedInputStream in) {
        bins = in;
    }

    public ChannelData readNextChunk(int block_size)  throws IOException {
        block_size *= 4;
        byte[] buffer = new byte[block_size];
        int read = bins.read(buffer);
        if (read == -1) {
            return null;
        } else if (read != block_size) {
            buffer = Arrays.copyOfRange(buffer, 0, read);
        }

        return SoundUtils.stereoBufferToChannelData(buffer, ByteOrder.LITTLE_ENDIAN);
    }

    public void close() throws IOException {

    }
}
