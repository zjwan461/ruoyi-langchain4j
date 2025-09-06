package com.ruoyi.ai.controller;

import com.ruoyi.ai.model.TranslateReq;
import com.ruoyi.ai.model.TranslateResp;
import com.ruoyi.ai.service.TranslateService;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.LimitType;
import javax.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/translate")
public class TranslateController extends BaseController {

  @Resource
  private TranslateService translateService;


  @PreAuthorize("@ss.hasPermi('tool:translate')")
  @PostMapping("/block")
  @RateLimiter(limitType = LimitType.IP, time = 60, count = 10)
  public AjaxResult translateBlock(@RequestBody TranslateReq translateReq) {
    String result = translateService.translateBlock(translateReq.getOrigin().concat("/no_think"),
        translateReq.getTargetLang());
    result = result.replaceAll("<think>\n\n</think>\n\n", "");
    TranslateResp resp = new TranslateResp();
    resp.setResult(result);
    return success(resp);
  }
}
