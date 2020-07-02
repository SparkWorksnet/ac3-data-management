package net.sparkworks.mapper.model.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MouseData {
    @JsonProperty("HeartRate")
    private Double heartRate;
    @JsonProperty("Temperature")
    private Double temperature;
    @JsonProperty("SkinResponse")
    private Double skinResponse;
    @JsonProperty("GripForce")
    private Double gripForce;
    @JsonProperty("IMU")
    private Imu imu;
    @JsonProperty("TimeStamp")
    private Long timestamp;
    @JsonProperty("Position")
    private Position position;
}
