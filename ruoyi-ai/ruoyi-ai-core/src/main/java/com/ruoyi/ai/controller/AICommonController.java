package com.ruoyi.ai.controller;

import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AICommonController {

    @Resource
    private AiConfig aiConfig;

    @GetMapping("/websearch-enable")
    public AjaxResult websearchEnable() {
        AiConfig.TavilyWebSearch tavilyWebSearch = aiConfig.getTavilyWebSearch();
        return AjaxResult.success(tavilyWebSearch.getEnabled());
    }


    @PostConstruct
    public void checkWebSearchConfig() {
        if (aiConfig.getTavilyWebSearch().getEnabled()) {
            TavilyWebSearchEngine searchEngine = null;
            try {
                searchEngine = TavilyWebSearchEngine.builder()
                        .apiKey(aiConfig.getTavilyWebSearch().getApiKey())
                        .build();
                searchEngine.search("just for test");
            } catch (Exception e) {
                log.warn("checkWebSearchConfig error. Will set websearch to disabled", e);
                aiConfig.getTavilyWebSearch().setEnabled(Boolean.FALSE);
            }

        }
    }
}
