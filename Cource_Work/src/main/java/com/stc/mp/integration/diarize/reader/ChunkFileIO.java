package com.stc.mp.integration.diarize.reader;

import com.stc.mp.integration.common.data.FileExt;
import com.stc.mp.integration.common.time.MuteTime;
import com.stc.mp.integration.common.utils.ChunkUtils;
import com.stc.mp.integration.common.wavheader.WavHeader;
import com.stc.mp.integration.common.wavheader.WavHeaderReader;
import com.stc.mp.integration.common.data.BaseAudioProperties;
import com.stc.mp.integration.diarize.data.ChunkData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static com.stc.mp.integration.common.utils.IOUtils.roundEven;


@Slf4j
@Data
public class ChunkFileIO {
    private static int BUF_SIZE = 10485760;

    public static ChunkData readStereoWav(File wavFile) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(wavFile), BUF_SIZE);
        in.skip(WavHeaderReader.HEADER_SIZE);
        return new ChunkData(in);
    }

    public static void writeMonoWavWithMuteTimes(File wavFile, List<MuteTime> muteTimes) throws IOException {
        WavHeader wavHeader = new WavHeaderReader(wavFile.getAbsolutePath()).read();
        wavHeader.setNumChannels((short) 1);
        File wavFileDia = Paths.get(wavFile.getAbsolutePath().replace(FileExt.WAV, "_dia" + FileExt.WAV)).toFile();

        long fileSize = Files.size(wavFile.toPath()) - WavHeaderReader.HEADER_SIZE;
        long fileSizeDia = fileSize / (wavHeader.getBitsPerSample() / 8) / wavHeader.getNumChannels();
        try (AudioInputStream audioInputStream = new AudioInputStream(new ByteArrayInputStream(new byte[0]), BaseAudioProperties.getAudioFormatMono(), fileSizeDia);
             InputStream inputStream = Files.newInputStream(wavFile.toPath(), StandardOpenOption.READ);
             OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(wavFileDia.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));) {

            if (muteTimes == null || muteTimes.isEmpty()) {
                Files.copy(wavFile.toPath(), new FileOutputStream(wavFileDia));
            } else {
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputStream);
                inputStream.skip(WavHeaderReader.HEADER_SIZE);

                long currentWrite = 0;
                long currentSkip = 0;
                long leftSize = fileSizeDia;
                MuteTime prevMuteTime = null;
                for (MuteTime muteTime : muteTimes) {
                    long writeBytes = roundEven((prevMuteTime == null ? muteTime.getStart() : muteTime.getStart() - prevMuteTime.getStop()) * BaseAudioProperties.getBytesInMillisec(), 4);
                    long skipBytes = roundEven((muteTime.getStop() - muteTime.getStart()) * BaseAudioProperties.getBytesInMillisec(), 4);

                    prevMuteTime = muteTime;

                    List<Integer> chunks = ChunkUtils.getChunkListSize(writeBytes);
                    for (Integer chunk : chunks) {
                        byte[] buffer = new byte[chunk];
                        inputStream.read(buffer);

                        int size = Math.min(chunk / 2, (int) leftSize);
                        byte[] newBuffer = new byte[size];
                        currentWrite += (size);
                        leftSize -= (size);

                        for (int i = 0; i < size / 2; i++) {
                            newBuffer[2 * i] = buffer[4 * i];
                            newBuffer[2 * i + 1] = buffer[4 * i + 1];
                        }
                        outputStream.write(newBuffer);
                    }

                    inputStream.skip(skipBytes);
                    chunks = ChunkUtils.getChunkListSize(skipBytes);
                    for (Integer chunk : chunks) {
                        int size = Math.min(chunk / 2, (int) leftSize);
                        currentSkip += (size);
                        leftSize -= (size);

                        byte[] buffer = new byte[size];
                        outputStream.write(buffer);
                    }
                }

                if (leftSize > 0) {
                    List<Integer> chunks = ChunkUtils.getChunkListSize(leftSize * 2);
                    for (Integer chunk : chunks) {
                        byte[] buffer = new byte[chunk];
                        inputStream.read(buffer);

                        int size = chunk / 2;
                        byte[] newBuffer = new byte[size];
                        currentWrite += (size);
                        leftSize -= (size);

                        for (int i = 0; i < size / 2; i++) {
                            newBuffer[2 * i] = buffer[4 * i];
                            newBuffer[2 * i + 1] = buffer[4 * i + 1];
                        }
                        outputStream.write(newBuffer);
                    }
                }

                log.info("Write Statistic:");
                System.out.println("Size: " + fileSize);
                System.out.println("Written: " + currentWrite);
                System.out.println("Skipped: " + currentSkip);
                System.out.println("SizeDia: " + fileSizeDia);

            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }
}
