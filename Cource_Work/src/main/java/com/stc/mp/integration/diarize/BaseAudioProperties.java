package com.stc.mp.integration.diarize;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFormat;

@Component
public class BaseAudioProperties {
    private static AudioFormat AUDIO_FORMAT;
    private static AudioFormat AUDIO_FORMAT_MONO;
    private static float BYTES_IN_MILLISEC;
    private static float BYTES_IN_MILLISEC_MONO;

    @Autowired
    public BaseAudioProperties(@Value("${channel.recorder.encoding}") String encoding,
                               @Value("${channel.recorder.sampleRate}") Float sampleRate,
                               @Value("${channel.recorder.sampleSizeInBits}") Integer sampleSizeInBits,
                               @Value("${channel.recorder.channels}") Integer channels,
                               @Value("${channel.recorder.frameSize}") Integer frameSize,
                               @Value("${channel.recorder.frameRate}") Float frameRate,
                               @Value("${channel.recorder.bigEndian}") Boolean bigEndian) {
        AUDIO_FORMAT = new AudioFormat(new AudioFormat.Encoding(encoding), sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
        AUDIO_FORMAT_MONO = new AudioFormat(new AudioFormat.Encoding(encoding), sampleRate, sampleSizeInBits, 1, frameSize, frameRate, bigEndian);
        BYTES_IN_MILLISEC = (AUDIO_FORMAT.getChannels() * AUDIO_FORMAT.getSampleRate() * AUDIO_FORMAT.getSampleSizeInBits()) / 8 / 1000;
        BYTES_IN_MILLISEC_MONO = (AUDIO_FORMAT_MONO.getChannels() * AUDIO_FORMAT_MONO.getSampleRate() * AUDIO_FORMAT_MONO.getSampleSizeInBits()) / 8 / 1000;
    }

    public static AudioFormat getAudioFormat() {
        return AUDIO_FORMAT;
    }

    public static AudioFormat getAudioFormatMono() {
        return AUDIO_FORMAT_MONO;
    }

    public static float getBytesInMillisec() {
        return BYTES_IN_MILLISEC;
    }

    public static float getBytesInMillisecMono() {
        return BYTES_IN_MILLISEC_MONO;
    }
}
