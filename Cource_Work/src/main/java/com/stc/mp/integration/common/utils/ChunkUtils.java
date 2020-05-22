package com.stc.mp.integration.common.utils;

import java.util.ArrayList;
import java.util.List;

public class ChunkUtils {
    private static int CHUNK_SIZE = 104857600;

    public static List<Integer> getChunkListSize(long size) {
        List<Integer> chunks = new ArrayList<>();
        int i = 1;
        while (CHUNK_SIZE * (long) i < size) {
            chunks.add(CHUNK_SIZE);
            i++;
        }
        chunks.add((int) size % CHUNK_SIZE);
        return chunks;
    }
}