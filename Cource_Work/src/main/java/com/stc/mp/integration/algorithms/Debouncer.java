package com.stc.mp.integration.algorithms;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.stc.mp.integration.algorithms.data.AvgStat;
import com.stc.mp.integration.algorithms.data.ChannelData;
import com.stc.mp.integration.algorithms.data.ChunkData;
import com.stc.mp.integration.algorithms.reader.ChunkFileIO;
import com.stc.mp.integration.common.data.FileExt;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static com.stc.mp.integration.common.utils.IOUtils.abssum;
import static com.stc.mp.integration.common.utils.SoundUtils.shortBuffersToByteStereoBuffer;
import static com.stc.mp.integration.common.utils.SoundUtils.stereoBufferToChannelData;

@Slf4j
public class Debouncer {
    private final int BUFFER;
    private final int WRITE_BUFFER;
    private final int CHECK_BUFFER;

    private final short thrSensor;

    private final boolean printAvgStats;
    private byte[] checkSoundBuffer;
    private int currentCheckSoundBufferSize;
    private float leftAverageLevel;
    private float rightAverageLevel;

    private CsvMapper csvMapper;

    public Debouncer() {
        this.WRITE_BUFFER = 8192;
        this.BUFFER = 64;
        this.thrSensor = (short) (Short.MAX_VALUE * 0.1);

        this.printAvgStats = true;
        this.CHECK_BUFFER = WRITE_BUFFER * 8;
        this.checkSoundBuffer = new byte[CHECK_BUFFER];
        this.currentCheckSoundBufferSize = 0;
        this.leftAverageLevel = 0;
        this.rightAverageLevel = 0;

        this.csvMapper = new CsvMapper();
    }


    public List<AvgStat> debounceFile(Path wavPath, String leftChannelId, String rightChannelId, float bytesInMilliSec) throws IOException {
        float blockSoundLength = (WRITE_BUFFER / bytesInMilliSec);
        List<AvgStat> avgStats = new ArrayList<>();
        File wavFile = wavPath.toFile();

        int i = 0;
        ChunkData chunkData = ChunkFileIO.readStereoWav(wavFile);
        for (ChannelData channelData = chunkData.readNextChunk(WRITE_BUFFER / 4); channelData != null; channelData = chunkData.readNextChunk(WRITE_BUFFER / 4)) {
            byte[] debouncedBuffer = debounce(channelData);
            if (debouncedBuffer.length == WRITE_BUFFER) {
                updateCheckBuffer(debouncedBuffer);
            }
            if (currentCheckSoundBufferSize == 0) {
                avgStats.add(new AvgStat(i * blockSoundLength * CHECK_BUFFER / WRITE_BUFFER / 1000, leftAverageLevel, rightAverageLevel));
                i++;
            }
        }

        if (printAvgStats) {
            Path statPath = Paths.get("C:", "voicecapture", "stats", wavFile.getName().replace(FileExt.WAV, FileExt.CSV));
            printAvgStats(avgStats, statPath, leftChannelId, rightChannelId);
        }

        return avgStats;
    }

    private byte[] debounce(ChannelData channelData) {
        short[] leftChannel = channelData.getLeftChannel();
        short[] rightChannel = channelData.getRightChannel();

        int n = leftChannel.length / BUFFER;
        for (int i = 0; i < n; i++) {
            short[] block = new short[BUFFER];
            System.arraycopy(rightChannel, i * BUFFER, block, 0, BUFFER);
            long coefficient = abssum(block);
            if (coefficient < thrSensor) {
                return new byte[WRITE_BUFFER];
            }
        }

        return shortBuffersToByteStereoBuffer(leftChannel, rightChannel);
    }

    private byte[] debounce(byte[] buffer) {
        ChannelData channelData = stereoBufferToChannelData(buffer, ByteOrder.LITTLE_ENDIAN);
        short[] leftChannel = channelData.getLeftChannel();
        short[] rightChannel = channelData.getRightChannel();

        int n = leftChannel.length / BUFFER;
        for (int i = 0; i < n; i++) {
            short[] block = new short[BUFFER];
            System.arraycopy(rightChannel, i * BUFFER, block, 0, BUFFER);
            long coefficient = abssum(block);
            if (coefficient < thrSensor) {
                return new byte[0];
            }
        }

        return shortBuffersToByteStereoBuffer(leftChannel, rightChannel);
    }

    private void updateCheckBuffer(byte[] buffer) {
        System.arraycopy(buffer, 0, checkSoundBuffer, currentCheckSoundBufferSize, buffer.length);
        currentCheckSoundBufferSize += buffer.length;

        if (currentCheckSoundBufferSize >= CHECK_BUFFER) {
            countAverageLevel(checkSoundBuffer);
            currentCheckSoundBufferSize = 0;
            checkSoundBuffer = new byte[CHECK_BUFFER];
        }
    }

    private void countAverageLevel(byte[] checkSoundBuffer) {
        ChannelData channelData = stereoBufferToChannelData(checkSoundBuffer, ByteOrder.LITTLE_ENDIAN);
        leftAverageLevel = (float) abssum(channelData.getLeftChannel()) / channelData.getLeftChannel().length;
        rightAverageLevel = (float) abssum(channelData.getRightChannel()) / channelData.getRightChannel().length;
    }

    private void printAvgStats(List<AvgStat> avgStats, Path statPath, String leftChannelId, String rightChannelId) throws IOException {
        CsvSchema schema = csvMapper.schemaFor(AvgStat.class)
                .withoutHeader()
                .withColumnSeparator(';')
                .withLineSeparator("\r\n");
        Files.createDirectories(statPath.getParent());
        try (OutputStream statStream = Files.newOutputStream(statPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            statStream.write(("Time;" + leftChannelId + ";" + rightChannelId + "\r\n").getBytes());
            csvMapper.writer(schema).withDefaultPrettyPrinter().writeValue(statStream, avgStats);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
