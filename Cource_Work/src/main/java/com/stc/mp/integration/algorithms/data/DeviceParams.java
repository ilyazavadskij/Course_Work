package com.stc.mp.integration.algorithms.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class DeviceParams {
    private String deviceId;
    private double amplifyLeft;
    private double amplifyRight;
    private double amplitude;
    private double min;
    private double max;

    public static DeviceParams fromStringWithDefault(String str, double amplifyLeft, double amplifyRight, double amplitude, double min, double max) {
        Map<String, String> properties = new HashMap<>();
        properties.put("amplifyLeft", String.valueOf(amplifyLeft));
        properties.put("amplifyRight", String.valueOf(amplifyRight));
        properties.put("amplitude", String.valueOf(amplitude));
        properties.put("min", String.valueOf(min));
        properties.put("max", String.valueOf(max));

        String[] parts = str.split(",");
        for (String part : parts) {
            String[] pair = part.split("=");
            properties.put(pair[0], pair[1]);
        }
        return new DeviceParams(properties.get("deviceId"), Double.parseDouble(properties.get("amplifyLeft")), Double.parseDouble(properties.get("amplifyRight")), Double.parseDouble(properties.get("amplitude")), Double.parseDouble(properties.get("min")), Double.parseDouble(properties.get("max")));
    }

    public static DeviceParams fromStringWithDefault(String str, double amplifyLeft, double amplifyRight, double min, double max) {
        return fromStringWithDefault(str, amplifyLeft, amplifyRight, max * Short.MAX_VALUE, min, max);
    }
}
