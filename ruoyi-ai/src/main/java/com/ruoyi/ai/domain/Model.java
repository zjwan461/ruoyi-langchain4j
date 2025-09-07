package com.ruoyi.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 模型管理对象 model
 * 
 * @author jerry
 * @date 2025-08-25
 */
public class Model extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模型ID */
    private Long id;

    /** 模型名称 */
    @Excel(name = "模型名称")
    private String name;

    /** base url */
    @Excel(name = "base url")
    private String baseUrl;

    /** api key */
    @Excel(name = "api key")
    private String apiKey;

    /** temperature */
    @Excel(name = "temperature")
    private Double temperature;

    /** token最大生成数 */
    @Excel(name = "token最大生成数")
    private Integer maxOutputToken;

    /** 类型 */
    @Excel(name = "类型")
    private Integer type;

    @Excel(name = "模型提供商")
    private String provider;

    @Excel(name = "保存目录")
    private String saveDir;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setBaseUrl(String baseUrl) 
    {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() 
    {
        return baseUrl;
    }

    public void setApiKey(String apiKey) 
    {
        this.apiKey = apiKey;
    }

    public String getApiKey() 
    {
        return apiKey;
    }

    public void setTemperature(Double temperature) 
    {
        this.temperature = temperature;
    }

    public Double getTemperature() 
    {
        return temperature;
    }

    public void setMaxOutputToken(Integer maxOutputToken) 
    {
        this.maxOutputToken = maxOutputToken;
    }

    public Integer getMaxOutputToken() 
    {
        return maxOutputToken;
    }

    public void setType(Integer type) 
    {
        this.type = type;
    }

    public Integer getType() 
    {
        return type;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSaveDir() {
        return saveDir;
    }

    public void setSaveDir(String saveDir) {
        this.saveDir = saveDir;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("baseUrl", getBaseUrl())
            .append("apiKey", getApiKey())
            .append("temperature", getTemperature())
            .append("maxOutputToken", getMaxOutputToken())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("type", getType())
            .append("provider", getProvider())
            .append("saveDir", getSaveDir())
            .toString();
    }
}
