package com.ruoyi.ai.model;

import javax.validation.constraints.NotBlank;

public class TranslateReq {

  @NotBlank
  private String origin;

  private String targetLang;

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public String getTargetLang() {
    return targetLang;
  }

  public void setTargetLang(String targetLang) {
    this.targetLang = targetLang;
  }
}
