package com.stc.mp.integration.diarize.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder(value = {"time", "leftChannel", "rightChannel"})
public class AvgStat {

    @JsonProperty("time")
    private float time;
    @JsonProperty("leftChannel")
    private float leftChannel;
    @JsonProperty("rightChannel")
    private float rightChannel;

    public String toString() {
        return String.format("AverageStat: %.3f: %.3f - %.3f", time, leftChannel, rightChannel);
    }
}
