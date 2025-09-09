package com.ruoyi.ai.controller.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class ChatReq {

  @NotNull
  private Long agentId;

  @NotBlank
  private String prompt;

  @NotBlank
  private String clientId;

  @NotBlank
  private String sessionId;

  public Long getAgentId() {
    return agentId;
  }

  public void setAgentId(Long agentId) {
    this.agentId = agentId;
  }

  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  @Override
  public String toString() {
    return "ChatReq{" +
        "agentId=" + agentId +
        ", prompt='" + prompt + '\'' +
        ", clientId='" + clientId + '\'' +
        ", sessionId='" + sessionId + '\'' +
        '}';
  }
}
