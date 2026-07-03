package com.ruoyi.ai.controller.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author jerry.su
 * @date 2025/9/13 23:45
 */
@Data
public class KbMatchReq {

    @NotNull
    private Long kbId;
    @NotBlank
    private String content;
    @NotNull
    private double minScore;
    @NotNull
    private int maxResult;
}
