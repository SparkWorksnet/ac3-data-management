package net.sparkworks.mapper.model.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MouseDataWrapper {
    @JsonProperty("MouseData")
    private MouseData mouseData;
    @JsonProperty("UserData")
    private UserData userData;
    @JsonProperty("ValidData")
    private boolean validData;
}
