package net.sparkworks.mapper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SingleValueReading {

  private Long timestamp;
  
  @JsonProperty(value = "Data")
  private Double reading;
  
}
