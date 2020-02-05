package net.sparkworks.mapper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ImuValueReading {
    
    private Long timestamp;
    
    @JsonProperty(value = "AcelX")
    private Double acelX;
    
    @JsonProperty(value = "AcelY")
    private Double acelY;
    
    @JsonProperty(value = "AcelZ")
    private Double acelZ;
    
    @JsonProperty(value = "GyroX")
    private Double gyroX;
    
    @JsonProperty(value = "GyroY")
    private Double gyroY;
    
    @JsonProperty(value = "GyroZ")
    private Double gyroZ;
    
    @JsonProperty(value = "CmpX")
    private Double cmpX;
    
    @JsonProperty(value = "CmpY")
    private Double cmpY;
    
    @JsonProperty(value = "CmpZ")
    private Double cmpZ;
}
