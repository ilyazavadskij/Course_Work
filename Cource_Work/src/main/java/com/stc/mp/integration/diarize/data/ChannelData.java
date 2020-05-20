package com.stc.mp.integration.diarize.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChannelData {

    short[] leftChannel;
    short[] rightChannel;
}
