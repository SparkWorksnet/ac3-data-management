package net.sparkworks.mapper.model;

import lombok.Data;

@Data
public class DoubleValueReading {
    
    private Long timestamp;
    
    private Double x;
    
    private Double y;
}
