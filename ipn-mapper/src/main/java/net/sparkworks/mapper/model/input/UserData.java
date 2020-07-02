package net.sparkworks.mapper.model.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserData {
    @JsonProperty("IsActive")
    private Boolean isActive;
    @JsonProperty("Hesitation")
    private Double hesitation;
    @JsonProperty("Frustration")
    private Double frustration;
    @JsonProperty("Fatigue")
    private Double fatigue;
    @JsonProperty("Stress")
    private Double stress;
    @JsonProperty("Neutral")
    private Double neutral;
    @JsonProperty("UUID")
    private String uuid;
}
