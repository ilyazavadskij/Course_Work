package com.stc.mp.integration.common.wavheader;

import lombok.Data;

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
        return "The RIFF chunk desriptor: " + new String(this.getChunkID()) + "\n" +
                "Size of this chunk: " + this.getChunkSize() + "\n" +
                "Format: " + new String(this.getFormat()) + "\n" + "\n" +
                "fmt subchunk: " + new String(this.getSubChunk1ID()) + "\n" +
                "Size of this chunk: " + this.getSubChunk1Size() + "\n" +
                "Audio format: " + this.getAudioFormat() + "\n" +
                "Number of channels: " + this.getNumChannels() + "\n" +
                "Sample rate: " + this.getSampleRate() + "\n" +
                "Byte rate: " + this.getByteRate() + "\n" +
                "Block align: " + this.getBlockAlign() + "\n" +
                "Bits per sample: " + this.getBitsPerSample() + "\n" + "\n" +
                "channel subchunk: " + new String(this.getSubChunk2ID()) + "\n" +
                "Size of this chunk: " + this.getSubChunk2Size();
    }

}