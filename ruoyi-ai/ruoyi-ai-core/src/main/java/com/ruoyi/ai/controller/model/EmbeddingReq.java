package com.ruoyi.ai.controller.model;

import javax.validation.constraints.NotNull;

public class EmbeddingReq extends DocSplitReq {

  @NotNull
  private Long kbId;

  @NotNull
  private Long embeddingModelId;

  public Long getKbId() {
    return kbId;
  }

  public void setKbId(Long kbId) {
    this.kbId = kbId;
  }

  public Long getEmbeddingModelId() {
    return embeddingModelId;
  }

  public void setEmbeddingModelId(Long embeddingModelId) {
    this.embeddingModelId = embeddingModelId;
  }
}
