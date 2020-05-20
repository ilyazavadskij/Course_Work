package com.stc.mp.integration.diarize.reader;

import com.stc.mp.integration.common.wavheader.WavHeaderReader;
import com.stc.mp.integration.diarize.data.ChunkData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Data
public class ChunkFileReader {
    private static int BUF_SIZE = 10485760;

    public static ChunkData readStereoWav(File wavFile) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(wavFile), BUF_SIZE);
        in.skip(WavHeaderReader.HEADER_SIZE);
        return new ChunkData(in);
    }
}
