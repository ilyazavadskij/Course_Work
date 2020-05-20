package com.stc.mp.integration.common.wavheader;

import lombok.Data;

import java.util.Arrays;

/*
 * @author: Matvey Timchenko
 * DateTime: 09.07.2018:10:39
 */

@Data
public class WavHeader {
    private byte[] chunkID = new byte[4];
    private int chunkSize;
    private byte[] format = new byte[4];
    private byte[] subChunk1ID = new byte[4];
    private int subChunk1Size;
    private short audioFormat;
    private short numChannels;
    private int sampleRate;
    private int byteRate;
    private short blockAlign;
    private short bitsPerSample;
    private byte[] subChunk2ID = new byte[4];
    private int subChunk2Size;

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("The RIFF chunk desriptor: ").append(new String(chunkID)).append("\n")
                .append("Size of this chunk: ").append(chunkSize).append("\n")
                .append("Format: ").append(new String(format)).append("\n")
                .append("fmt subchunk: ").append(new String(subChunk1ID)).append("\n")
                .append("Size of this chunk: ").append(subChunk1Size).append("\n")
                .append("Audio format: ").append(audioFormat).append("\n")
                .append("Number of channels: ").append(numChannels).append("\n")
                .append("Sample rate: ").append(sampleRate).append("\n")
                .append("Byte rate: ").append(byteRate).append("\n")
                .append("Block align: ").append(blockAlign).append("\n")
                .append("Bits per sample: ").append(bitsPerSample).append("\n")
                .append("channel subchunk: ").append(new String(subChunk2ID)).append("\n")
                .append("Size of this chunk: ").append(subChunk2Size);

        return stringBuilder.toString();
    }

}