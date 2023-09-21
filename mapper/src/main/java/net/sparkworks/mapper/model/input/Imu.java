package net.sparkworks.mapper.model.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Imu {
    @JsonProperty("AcelX")
    private Double acelX;
    @JsonProperty("AcelY")
    private Double acelY;
    @JsonProperty("AcelZ")
    private Double acelZ;
    @JsonProperty("GyroX")
    private Double gyroX;
    @JsonProperty("GyroY")
    private Double gyroY;
    @JsonProperty("GyroZ")
    private Double gyroZ;
    @JsonProperty("CmpX")
    private Double cmpX;
    @JsonProperty("CmpY")
    private Double cmpY;
    @JsonProperty("CmpZ")
    private Double cmpZ;
}
