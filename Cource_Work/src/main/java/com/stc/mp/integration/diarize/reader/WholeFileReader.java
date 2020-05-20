package com.stc.mp.integration.diarize.reader;

import com.stc.mp.integration.common.wavheader.WavHeaderReader;
import com.stc.mp.integration.diarize.data.ChannelData;
import com.stc.mp.integration.common.utils.SoundUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.ByteOrder;

@Slf4j
public class WholeFileReader {
    public static ChannelData readStereoWav(String wavFile) {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(wavFile))) {
            in.skip(WavHeaderReader.HEADER_SIZE);

            byte[] buffer = new byte[in.available()];
            in.read(buffer);

            return SoundUtils.stereoBufferToChannelData(buffer, ByteOrder.LITTLE_ENDIAN);
        } catch(Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
        return null;
    }
}
