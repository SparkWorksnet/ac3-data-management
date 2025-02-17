package net.sparkworks.mapper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EnvMessage {
    String sensorid;
    Long timestamp;
    Double temperature;
    Double humidity;
    Double iaq;
    @JsonProperty("iaq_accuracy")
    Double iaqAccuracy;
    Double co2;
}
