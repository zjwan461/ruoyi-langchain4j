package com.ruoyi.ai.dify.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DifyInfoResponse {

    private String name;
    private String description;
    private List<String> tags;
    private String mode;
    @JsonProperty("author_name")
    private String authorName;
}
