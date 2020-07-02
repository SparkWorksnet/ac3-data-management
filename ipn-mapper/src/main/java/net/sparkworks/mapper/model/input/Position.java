package net.sparkworks.mapper.model.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Position {
    @JsonProperty("X")
    private Double x;
    @JsonProperty("Y")
    private Double y;
}
