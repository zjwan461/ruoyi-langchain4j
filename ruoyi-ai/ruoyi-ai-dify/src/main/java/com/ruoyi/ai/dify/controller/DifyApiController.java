package com.ruoyi.ai.dify.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.ai.dify.domain.DifyApi;
import com.ruoyi.ai.dify.service.IDifyApiService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * Dify APIController
 * 
 * @author ruoyi
 * @date 2025-10-27
 */
@RestController
@RequestMapping("/ai/difyApi")
public class DifyApiController extends BaseController
{
    @Autowired
    private IDifyApiService difyApiService;

    /**
     * 查询Dify API列表
     */
    @PreAuthorize("@ss.hasPermi('ai:difyApi:list')")
    @GetMapping("/list")
    public TableDataInfo list(DifyApi difyApi)
    {
        startPage();
        List<DifyApi> list = difyApiService.selectDifyApiList(difyApi);
        return getDataTable(list);
    }

    /**
     * 导出Dify API列表
     */
    @PreAuthorize("@ss.hasPermi('ai:difyApi:export')")
    @Log(title = "Dify API", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DifyApi difyApi)
    {
        List<DifyApi> list = difyApiService.selectDifyApiList(difyApi);
        ExcelUtil<DifyApi> util = new ExcelUtil<DifyApi>(DifyApi.class);
        util.exportExcel(response, list, "Dify API数据");
    }

    /**
     * 获取Dify API详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:difyApi:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(difyApiService.selectDifyApiById(id));
    }

    /**
     * 新增Dify API
     */
    @PreAuthorize("@ss.hasPermi('ai:difyApi:add')")
    @Log(title = "Dify API", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DifyApi difyApi)
    {
        return toAjax(difyApiService.insertDifyApi(difyApi));
    }

    /**
     * 修改Dify API
     */
    @PreAuthorize("@ss.hasPermi('ai:difyApi:edit')")
    @Log(title = "Dify API", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DifyApi difyApi)
    {
        return toAjax(difyApiService.updateDifyApi(difyApi));
    }

    /**
     * 删除Dify API
     */
    @PreAuthorize("@ss.hasPermi('ai:difyApi:remove')")
    @Log(title = "Dify API", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(difyApiService.deleteDifyApiByIds(ids));
    }
}
