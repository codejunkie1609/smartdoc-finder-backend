package com.smartdocfinder.core.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MultiEmbeddingResponse {
    private List<Float> base;
    private List<List<Float>> sub;
}
