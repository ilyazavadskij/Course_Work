package com.stc.mp.integration.diarize;

import com.google.common.collect.EvictingQueue;
import com.stc.mp.integration.common.data.FileExt;
import com.stc.mp.integration.common.utils.ChunkUtil;
import com.stc.mp.integration.common.wavheader.WavHeader;
import com.stc.mp.integration.common.wavheader.WavHeaderReader;
import com.stc.mp.integration.diarize.data.ChannelData;
import com.stc.mp.integration.diarize.data.ChunkData;
import com.stc.mp.integration.diarize.data.DeviceParams;
import com.stc.mp.integration.common.time.MuteTime;
import com.stc.mp.integration.diarize.reader.ChunkFileReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static com.stc.mp.integration.common.utils.SoundUtils.roundEven;

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
        muteTimes.forEach(System.out::println);

        writeWavFile(wavPath.toFile(), muteTimes);
    }


    static void writeWavFile(File wavFile, List<MuteTime> muteTimes) throws IOException {
        WavHeader wavHeader = new WavHeaderReader(wavFile.getAbsolutePath()).read();
        WavHeader wavHeaderDia = new WavHeaderReader(wavFile.getAbsolutePath()).read();
        File wavFileDia = Paths.get(wavFile.getAbsolutePath().replace(FileExt.WAV, "_dia" + FileExt.WAV)).toFile();
        System.out.println(wavFileDia);

        wavHeaderDia.setNumChannels((short) 1);
        wavHeaderDia.setSubChunk2Size(wavHeaderDia.getSubChunk2Size() / 2);

        long fileSize = Files.size(wavFile.toPath());
        fileSize = (fileSize - WavHeaderReader.HEADER_SIZE) / (wavHeaderDia.getBitsPerSample() / 8) / wavHeaderDia.getNumChannels();
        try (AudioInputStream audioInputStream = new AudioInputStream(new ByteArrayInputStream(new byte[0]), BaseAudioProperties.getAudioFormatMono(), fileSize);
             InputStream inputStream = Files.newInputStream(wavFile.toPath(), StandardOpenOption.READ);
             OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(wavFileDia.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));) {

            if (muteTimes == null || muteTimes.isEmpty()) {
                Files.copy(wavFile.toPath(), new FileOutputStream(wavFileDia));
            } else {
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputStream);
                inputStream.skip(WavHeaderReader.HEADER_SIZE);

                MuteTime prevMuteTime = null;
                for (MuteTime muteTime : muteTimes) {
                    long writeBytes = (int) roundEven((prevMuteTime == null ? muteTime.getStart() : muteTime.getStart() - prevMuteTime.getStop()) * BaseAudioProperties.getBytesInMillisec(), 4);
                    long skipBytes = roundEven((muteTime.getStop() - muteTime.getStart()) * BaseAudioProperties.getBytesInMillisec(), 4);

                    prevMuteTime = muteTime;

                    List<Integer> chunks = ChunkUtil.getChunkListSize(writeBytes);
                    for (Integer chunk : chunks) {
                        byte[] buffer = new byte[chunk];
                        inputStream.read(buffer);
                        byte[] newBuffer = new byte[chunk / 2];
                        for (int i = 0; i < chunk / 4; i++) {
                            newBuffer[2 * i] = buffer[4 * i];
                            newBuffer[2 * i + 1] = buffer[4 * i + 1];
                        }
                        outputStream.write(newBuffer);
                    }

                    inputStream.skip(skipBytes);

                    chunks = ChunkUtil.getChunkListSize(skipBytes);
                    for (Integer chunk : chunks) {
                        byte[] buffer = new byte[chunk / 2];
                        outputStream.write(buffer);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    public List<MuteTime> diarizeFile(Path wavPath, String deviceId) throws IOException {
        File wavFile = wavPath.toFile();
        ChunkData chunkData = ChunkFileReader.readStereoWav(wavFile);

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

        short[] prvBlock = zeros(BLOCK_SIZE);

        for (ChannelData chd = chunkData.readNextChunk(BLOCK_SIZE); chd != null; chd = chunkData.readNextChunk(BLOCK_SIZE)) {
            short[] s1 = chd.getRightChannel();
            short[] s2 = chd.getLeftChannel();

//            short[] ogib1 = abs(s1, balancingThreshold);               //построение огибающей ненаправленного канала
//            short[] ogib2 = abs(s2, 1d);             //построение огибающей направленного канала
//
//            long k2 = sum(ogib2);
            long k1 = abssum(s1, deviceParams.getAmplifyRight());
//            long k1 = sum(ogib1);
            long k2 = abssum(s2, deviceParams.getAmplifyLeft());

            long prev = sum(prvBlock, BLOCK_SIZE - 10); //prv[prv.length - 4];

            short[] currentBlock;
            if (k1 >= k2 || k2 < deviceParams.getAmplitude() || (k2 > (deviceParams.getMin() * k1) && k2 < (deviceParams.getMax() * k1))) {
                currentBlock = zeros(BLOCK_SIZE);
                blockStates.add(Boolean.FALSE);
            } else {
                blockStates.add(Boolean.TRUE);
                currentBlock = s2;
            }

            prvBlock = currentBlock;

            //повышает помехоустойчивость
            //(обнуляет, если торчит только один ненулевой блок между двумя нулевыми)
            ks.add(sum(currentBlock));
            Long[] ksArray = new Long[3];
            ks.toArray(ksArray);
            if (ksArray[0] == 0 && ksArray[2] == 0 && ksArray[1] != 0) {
                prvBlock = zeros(BLOCK_SIZE);
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


    private short[] zeros(int size) {
        return new short[size];
//        Arrays.fill(ar, (short) 0);
//        return ar;
    }

    private long sum(short[] data, int from) {
        long sum = 0;
        for (int i = from; i < data.length - 5; i++) {
            sum += data[i];
        }
        return sum;
    }

    private long sum(short[] data) {
        long sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        return sum;
    }

    private short[] abs(short[] data, double k) {
        short[] resultData = new short[data.length];
        for (int i = 0; i < data.length; i++) {
            resultData[i] = (short) Math.abs(data[i] * k);
        }
        return resultData;
    }

    private long abssum(short[] data, double k) {
        long sum = 0;
        for (short datum : data) {
            sum += (short) Math.abs(datum * k);
        }
        return sum;
    }
}
