package com.ruoyi.ai.enums;

public enum ModelProvider {

  OLLAMA("Ollama"), OPEN_AI("Open AI");

  private final String value;

  ModelProvider(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static ModelProvider fromValue(String value) {
    for (ModelProvider modelProvider : ModelProvider.values()) {
      if (modelProvider.getValue().equals(value)) {
        return modelProvider;
      }
    }
    throw new IllegalArgumentException("not support value for ModelProvider: " + value);
  }
}
