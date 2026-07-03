package com.ruoyi.ai.dify.api;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

@Component
@Slf4j
public class RequestUtil {

    @Resource
    private ObjectMapper objectMapper;


    public DifyInfoResponse info(String urlPrefix, String apiKey) {
        if (StringUtils.isBlank(urlPrefix)) {
            throw new ServiceException("dify url prefix is empty");
        }
        HttpRequest httpRequest = HttpUtil.createRequest(Method.GET, urlPrefix + "/v1/info");
        if (StringUtils.isNotBlank(apiKey)) {
            httpRequest.header("Authorization", "Bearer " + apiKey);
        }
        HttpResponse response = httpRequest.execute();
        if (!response.isOk()) {
            throw new ServiceException("request dify return http status=" + response.getStatus());
        }
        String body = response.body();
        try {
            return objectMapper.readValue(body, DifyInfoResponse.class);
        } catch (JsonProcessingException e) {
            log.error("format dify response to json fail...", e);
            throw new ServiceException("format dify response to json fail...");
        }
    }

    public DifyChatBlockingResponse chatBlocking(String urlPrefix, String apiKey, DifyChatRequest request) {
        if (StringUtils.isBlank(urlPrefix)) {
            throw new ServiceException("dify url prefix is empty");
        }
        if (Objects.isNull(request)) {
            throw new ServiceException("dify request is null");
        }
        request.setResponseMode(DifyChatRequest.ResponseModel.blocking);

        String content = null;
        try {
            content = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("format dify request to json fail...", e);
            throw new ServiceException("format dify request to json fail...");
        }

        HttpRequest httpRequest = HttpUtil.createRequest(Method.POST, urlPrefix + "/v1/chat/messages")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(content);

        if (StringUtils.isNotBlank(apiKey)) {
            httpRequest.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse response = httpRequest.execute();
        if (!response.isOk()) {
            throw new ServiceException("request dify return http status=" + response.getStatus());
        }
        String body = response.body();
        try {
            return objectMapper.readValue(body, DifyChatBlockingResponse.class);
        } catch (JsonProcessingException e) {
            log.error("format dify response to json fail...", e);
            throw new ServiceException("format dify response to json fail...");
        }
    }
}
