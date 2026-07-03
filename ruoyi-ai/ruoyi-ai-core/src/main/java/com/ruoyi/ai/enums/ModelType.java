package com.ruoyi.ai.enums;

public enum ModelType {

  LLM(0), EMBEDDING(1),
  ;

  private final int value;

  ModelType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static ModelType fromValue(int value) {
    for (ModelType modelType : values()) {
      if (modelType.getValue() == value) {
        return modelType;
      }
    }
    throw new IllegalArgumentException("not support value for ModelType: " + value);
  }
}
