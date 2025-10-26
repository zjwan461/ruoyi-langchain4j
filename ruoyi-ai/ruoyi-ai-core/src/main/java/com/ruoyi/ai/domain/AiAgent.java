package com.ruoyi.ai.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * AI智能体对象 ai_agent
 *
 * @author jerry
 * @date 2025-09-02
 */
public class AiAgent extends BaseEntity {

  private static final long serialVersionUID = 1L;

  /**
   * 主键ID
   */
  private Long id;

  /**
   * 应用名
   */
  @Excel(name = "应用名")
  private String name;

  /**
   * 知识库ID
   */
  @Excel(name = "知识库ID")
  private String kbIds;

  @Excel(name = "知识库名")
  private String kbNames;

  /**
   * 系统提示词
   */
  @Excel(name = "系统提示词")
  private String systemMessage;

  /**
   * 记忆轮次
   */
  @Excel(name = "记忆轮次")
  private Integer memoryCount;

  /**
   * 模型ID
   */
  @Excel(name = "模型ID")
  private Long modelId;

  @Excel(name = "模型名")
  private String modelName;

  /**
   * 状态
   */
  @Excel(name = "状态")
  private Integer status;

  /**
   * 访问链接
   */
  @Excel(name = "访问链接")
  private String visitUrl;

  /**
   * 单个Client每日访问限制次数
   */
  @Excel(name = "单个Client每日访问限制次数")
  private Integer dayLmtPerClient;

  /**
   * temperature
   */
  @Excel(name = "temperature")
  private Double temperature;

  /**
   * token最大生成数
   */
  @Excel(name = "token最大生成数")
  private Integer maxOutputToken;

  @Excel(name = "提示词模板")
  private String promptTemplate;

  @Excel(name = "知识库查询最小得分")
  private Double minScore;

  @Excel(name = "知识库查询最多条数")
  private Integer maxResult;

  public void setId(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }



  public void setSystemMessage(String systemMessage) {
    this.systemMessage = systemMessage;
  }

  public String getSystemMessage() {
    return systemMessage;
  }

  public void setMemoryCount(Integer memoryCount) {
    this.memoryCount = memoryCount;
  }

  public Integer getMemoryCount() {
    return memoryCount;
  }

  public void setModelId(Long modelId) {
    this.modelId = modelId;
  }

  public Long getModelId() {
    return modelId;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Integer getStatus() {
    return status;
  }

  public void setVisitUrl(String visitUrl) {
    this.visitUrl = visitUrl;
  }

  public String getVisitUrl() {
    return visitUrl;
  }

  public Integer getDayLmtPerClient() {
    return dayLmtPerClient;
  }

  public void setDayLmtPerClient(Integer dayLmtPerClient) {
    this.dayLmtPerClient = dayLmtPerClient;
  }

  public String getKbIds() {
    return kbIds;
  }

  public void setKbIds(String kbIds) {
    this.kbIds = kbIds;
  }

  public String getKbNames() {
    return kbNames;
  }

  public void setKbNames(String kbNames) {
    this.kbNames = kbNames;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public Double getTemperature() {
    return temperature;
  }

  public void setTemperature(Double temperature) {
    this.temperature = temperature;
  }

  public Integer getMaxOutputToken() {
    return maxOutputToken;
  }

  public void setMaxOutputToken(Integer maxOutputToken) {
    this.maxOutputToken = maxOutputToken;
  }

  public String getPromptTemplate() {
    return promptTemplate;
  }

  public void setPromptTemplate(String promptTemplate) {
    this.promptTemplate = promptTemplate;
  }

  public Double getMinScore() {
    return minScore;
  }

  public void setMinScore(Double minScore) {
    this.minScore = minScore;
  }

  public Integer getMaxResult() {
    return maxResult;
  }

  public void setMaxResult(Integer maxResult) {
    this.maxResult = maxResult;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
        .append("id", getId())
        .append("name", getName())
        .append("kbIds", getKbIds())
        .append("systemMessage", getSystemMessage())
        .append("memoryCount", getMemoryCount())
        .append("modelId", getModelId())
        .append("createBy", getCreateBy())
        .append("createTime", getCreateTime())
        .append("updateBy", getUpdateBy())
        .append("updateTime", getUpdateTime())
        .append("status", getStatus())
        .append("visitUrl", getVisitUrl())
        .append("dayLmtPerClient", getDayLmtPerClient())
        .append("modelName", getModelName())
        .append("kbNames", getKbNames())
        .append("temperature", getTemperature())
        .append("maxOutputToken", getMaxOutputToken())
        .append("promptTemplate", getPromptTemplate())
        .append("minScore", getMinScore())
        .append("maxResult", getMaxResult())
        .toString();
  }


}
