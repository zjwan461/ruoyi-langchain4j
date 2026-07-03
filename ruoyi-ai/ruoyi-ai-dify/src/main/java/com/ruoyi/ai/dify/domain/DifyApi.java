package com.ruoyi.ai.dify.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * Dify API对象 dify_api
 * 
 * @author ruoyi
 * @date 2025-10-27
 */
public class DifyApi extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** Dify APl前缀 */
    @Excel(name = "Dify APl前缀")
    private String urlPrefix;

    /** api_key */
    @Excel(name = "api_key")
    private String apiKey;

    /** 响应类型 */
    @Excel(name = "响应类型")
    private String responseType;

    /** 单个Client每日访问限制次数 */
    @Excel(name = "单个Client每日访问限制次数")
    private Integer dayLmtPerClient;

    /** 应用名 */
    @Excel(name = "应用名")
    private String name;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUrlPrefix(String urlPrefix) 
    {
        this.urlPrefix = urlPrefix;
    }

    public String getUrlPrefix() 
    {
        return urlPrefix;
    }

    public void setApiKey(String apiKey) 
    {
        this.apiKey = apiKey;
    }

    public String getApiKey() 
    {
        return apiKey;
    }

    public void setResponseType(String responseType) 
    {
        this.responseType = responseType;
    }

    public String getResponseType() 
    {
        return responseType;
    }

    public void setDayLmtPerClient(Integer dayLmtPerClient) 
    {
        this.dayLmtPerClient = dayLmtPerClient;
    }

    public Integer getDayLmtPerClient() 
    {
        return dayLmtPerClient;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("urlPrefix", getUrlPrefix())
            .append("apiKey", getApiKey())
            .append("responseType", getResponseType())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("dayLmtPerClient", getDayLmtPerClient())
            .append("name", getName())
            .toString();
    }
}
