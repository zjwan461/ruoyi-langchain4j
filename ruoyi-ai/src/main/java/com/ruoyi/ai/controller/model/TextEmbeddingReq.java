package com.ruoyi.ai.controller.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class TextEmbeddingReq {

  @NotNull
  private Long kbId;

  @NotNull
  private Long embeddingModelId;

  @NotBlank
  private String text;

  private String embeddingId;


  public @NotNull Long getKbId() {
    return kbId;
  }

  public void setKbId(@NotNull Long kbId) {
    this.kbId = kbId;
  }

  public @NotNull Long getEmbeddingModelId() {
    return embeddingModelId;
  }

  public void setEmbeddingModelId(@NotNull Long embeddingModelId) {
    this.embeddingModelId = embeddingModelId;
  }

  public @NotBlank String getText() {
    return text;
  }

  public void setText(@NotBlank String text) {
    this.text = text;
  }

  public String getEmbeddingId() {
    return embeddingId;
  }

  public void setEmbeddingId(String embeddingId) {
    this.embeddingId = embeddingId;
  }
}
