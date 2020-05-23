package com.stc.mp.integration.algorithms.data;

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
