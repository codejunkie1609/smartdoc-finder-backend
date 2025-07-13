package com.smartdocfinder.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Maps the JSON response from the Python generator service.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratorResponse {
    private String answer;
    private String error;
}
