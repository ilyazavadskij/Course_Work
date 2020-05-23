package com.stc.mp.integration.algorithms;

import com.google.common.collect.EvictingQueue;
import com.stc.mp.integration.common.data.FileExt;
import com.stc.mp.integration.common.wavheader.WavHeader;
import com.stc.mp.integration.common.wavheader.WavHeaderReader;
import com.stc.mp.integration.algorithms.data.ChannelData;
import com.stc.mp.integration.algorithms.data.ChunkData;
import com.stc.mp.integration.algorithms.data.DeviceParams;
import com.stc.mp.integration.common.time.MuteTime;
import com.stc.mp.integration.algorithms.reader.ChunkFileIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.stc.mp.integration.common.utils.IOUtils.abssum;

@Component
@Slf4j
public class Diarizer {
    private double amplifyLeft;
    private double amplifyRight;
    private double amplitudeThreshold;
    private double minThreshold;
    private double maxThreshold;

    private short BLOCK_SIZE;                    //размер буфера анализа

    private List<String> params;
    private Map<String, DeviceParams> defaultDeviceParams;

    @Autowired
    public Diarizer(@Value("${diarizer.threshold.amplifyLeft}") double amplifyLeft,
                    @Value("${diarizer.threshold.amplifyRight}") double amplifyRight,
                    @Value("${diarizer.threshold.min}") double minThreshold,
                    @Value("${diarizer.threshold.max}") double maxThreshold,
                    @Value("#{'${diarizer.threshold.device.params}'.split(';')}") List<String> params) {
        this.amplifyLeft = amplifyLeft;
        this.amplifyRight = amplifyRight;
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
        this.amplitudeThreshold = maxThreshold * Short.MAX_VALUE;
        this.BLOCK_SIZE = 4096;

        this.params = params;
        this.defaultDeviceParams = new HashMap<>();
    }

    @PostConstruct
    public void preProcess() {
        for (String param : params) {
            DeviceParams deviceParams = DeviceParams.fromStringWithDefault(param, amplifyLeft, amplifyRight, minThreshold, maxThreshold);

            if (!deviceParams.getDeviceId().isEmpty()) {
                log.debug("Default params for device({}): {}", deviceParams.getDeviceId(), deviceParams);
                defaultDeviceParams.put(deviceParams.getDeviceId(), deviceParams);
            }
        }
    }

    public void process() throws Exception {
        log.debug("In Diarizer");
        String wavURI = "src/main/resources/Audio" + FileExt.WAV;
        Path wavPath = Paths.get(wavURI);

        List<MuteTime> muteTimes = diarizeFile(wavPath, null);
        log.info("Mute Times:");
        muteTimes.forEach(System.out::println);

        ChunkFileIO.writeMonoWavWithMuteTimes(wavPath.toFile(), muteTimes);
    }

    public List<MuteTime> diarizeFile(Path wavPath, String deviceId) throws IOException {
        File wavFile = wavPath.toFile();
        ChunkData chunkData = ChunkFileIO.readStereoWav(wavFile);

        WavHeader wavHeader = new WavHeaderReader(wavFile.getAbsolutePath()).read();
        log.info("{} HEADER\n{}", wavPath.getFileName(), wavHeader);
        float sampleRate = wavHeader.getSampleRate();

        log.debug("WavFile ({}) is diarizing", wavFile.getAbsolutePath());
        return diarize(chunkData, deviceId, sampleRate);
    }

    private List<MuteTime> diarize(ChunkData chunkData, String deviceId, float sampleRate) throws IOException {
        float blockSoundLength = BLOCK_SIZE * 1000 / sampleRate;

        DeviceParams deviceParams;
        if (defaultDeviceParams.containsKey(deviceId)) {
            deviceParams = defaultDeviceParams.get(deviceId);
        } else {
            deviceParams = new DeviceParams(deviceId, amplifyLeft, amplifyRight, amplitudeThreshold, minThreshold, maxThreshold);
        }
        log.debug("    With params: {}", deviceParams);

        List<MuteTime> muteTimes = new ArrayList<>();

        List<Boolean> blockStates = new ArrayList<>();
        Queue<Long> ks = EvictingQueue.create(3);
        ks.add(0L);
        ks.add(0L);
        ks.add(0L);

        for (ChannelData chd = chunkData.readNextChunk(BLOCK_SIZE); chd != null; chd = chunkData.readNextChunk(BLOCK_SIZE)) {
            short[] s1 = chd.getRightChannel();
            short[] s2 = chd.getLeftChannel();

            long k1 = abssum(s1, deviceParams.getAmplifyRight());
            long k2 = abssum(s2, deviceParams.getAmplifyLeft());

            if ((k1 >= k2) || (k2 < deviceParams.getAmplitude()) || (k2 > (deviceParams.getMin() * k1) && k2 < (deviceParams.getMax() * k1))) {
                blockStates.add(Boolean.FALSE);
                ks.add((long) 0);
            } else {
                blockStates.add(Boolean.TRUE);
                ks.add(k1);
            }

            Long[] ksArray = new Long[3];
            ks.toArray(ksArray);
            if (ksArray[0] == 0 && ksArray[2] == 0 && ksArray[1] != 0) {
                blockStates.set(blockStates.size() - 2, Boolean.FALSE);
            }
        }

        int i = 0;
        while (i < blockStates.size()) {
            if (!blockStates.get(i)) {
                MuteTime muteTime = new MuteTime();
                muteTime.setStart((long) (i * blockSoundLength));
                while (i < blockStates.size() && !blockStates.get(i)) {
                    i++;
                }
                muteTime.setStop((long) (i * blockSoundLength));
                muteTimes.add(muteTime);
            }
            i++;
        }
        return muteTimes;
    }
}
