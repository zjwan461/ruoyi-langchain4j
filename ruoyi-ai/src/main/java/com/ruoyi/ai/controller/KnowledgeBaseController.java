package com.ruoyi.ai.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.ai.controller.model.DocSplitReq;
import com.ruoyi.ai.controller.model.EmbeddingReq;
import com.ruoyi.ai.controller.model.TextEmbeddingReq;
import com.ruoyi.ai.domain.KnowledgeBase;
import com.ruoyi.ai.domain.Model;
import com.ruoyi.ai.service.IKnowledgeBaseService;
import com.ruoyi.ai.service.IModelService;
import com.ruoyi.ai.service.LangChain4jService;
import com.ruoyi.ai.service.impl.ModelBuilder;
import com.ruoyi.ai.util.Constants;
import com.ruoyi.ai.util.PgVectorUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.framework.websocket.SocketMessage;
import com.ruoyi.framework.websocket.WebSocketUsers;
import com.ruoyi.system.service.ISysConfigService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.websocket.Session;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库Controller
 *
 * @author jerry
 * @date 2025-08-25
 */
@RestController
@RequestMapping("/ai/knowledgeBase")
public class KnowledgeBaseController extends BaseController {


    @Autowired
    private IKnowledgeBaseService knowledgeBaseService;

    @Autowired
    private LangChain4jService langChain4jService;

    @Autowired
    private IModelService modelService;

    @Autowired
    private PgVectorUtil pgVectorUtil;

    @Autowired
    private ModelBuilder modelBuilder;

    @Autowired
    private ISysConfigService sysConfigService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 查询知识库列表
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledgeBase:list')")
    @GetMapping("/list")
    public TableDataInfo list(KnowledgeBase knowledgeBase) {
        startPage();
        List<KnowledgeBase> list = knowledgeBaseService.selectKnowledgeBaseList(knowledgeBase);
        return getDataTable(list);
    }

    /**
     * 导出知识库列表
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledgeBase:export')")
    @Log(title = "知识库", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, KnowledgeBase knowledgeBase) {
        List<KnowledgeBase> list = knowledgeBaseService.selectKnowledgeBaseList(knowledgeBase);
        ExcelUtil<KnowledgeBase> util = new ExcelUtil<KnowledgeBase>(KnowledgeBase.class);
        util.exportExcel(response, list, "知识库数据");
    }

    /**
     * 获取知识库详细信息
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledgeBase:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(knowledgeBaseService.selectKnowledgeBaseById(id));
    }

    /**
     * 新增知识库
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledgeBase:add')")
    @Log(title = "知识库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody KnowledgeBase knowledgeBase) {
        knowledgeBase.setCreateBy(getUsername());
        return toAjax(knowledgeBaseService.insertKnowledgeBase(knowledgeBase));
    }

    /**
     * 修改知识库
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledgeBase:edit')")
    @Log(title = "知识库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody KnowledgeBase knowledgeBase) {
        knowledgeBase.setUpdateBy(getUsername());
        return toAjax(knowledgeBaseService.updateKnowledgeBase(knowledgeBase));
    }

    /**
     * 删除知识库
     */
    @PreAuthorize("@ss.hasPermi('ai:knowledgeBase:remove')")
    @Log(title = "知识库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(knowledgeBaseService.deleteKnowledgeBaseByIds(ids));
    }

    @PreAuthorize("@ss.hasPermi('ai:knowledgeBase:list')")
    @PostMapping("/doc-split")
    public AjaxResult docSplit(@RequestBody DocSplitReq docSplitReq) {
        List<TextSegment> textSegments = docSplitSegment(
                docSplitReq);
        return success(textSegments.stream().map(TextSegment::text).collect(Collectors.toList()));
    }

    private List<TextSegment> docSplitSegment(DocSplitReq docSplitReq) {
        LinkedHashMap<String, Object> file = docSplitReq.getFileList().get(0);
        String fileName = (String) ((HashMap) file.get("response")).get("fileName");
        fileName = fileName.replace("/profile", RuoYiConfig.getProfile());

        return langChain4jService.splitDocument(fileName,
                docSplitReq.getMaxSegmentSize(),
                docSplitReq.getMaxOverlapSize());
    }

    @PreAuthorize("@ss.hasPermi('ai:knowledgeBase:list')")
    @Log(title = "文档向量化", businessType = BusinessType.INSERT)
    @PostMapping("/embedding")
    public AjaxResult embedding(@RequestBody @Valid EmbeddingReq embeddingReq) {
        Long embeddingModelId = embeddingReq.getEmbeddingModelId();
        Model model = modelService.selectModelById(embeddingModelId);
        EmbeddingModel embeddingModel = modelBuilder.getEmbeddingModel(model);
        List<TextSegment> segments = docSplitSegment(embeddingReq);
        segments.forEach(segment -> {
            Metadata metadata = segment.metadata();
            metadata.put(Constants.KB_ID, embeddingReq.getKbId());
        });
        String batchSize = sysConfigService.selectConfigByKey("ai.embedding.batchSize");
        String token = tokenService.getToken(request);
        AsyncManager.me().execute(new TimerTask() {
            @Override
            public void run() {
                langChain4jService.embedTextSegments(embeddingModel, segments, textSegments -> {
                    Session session = WebSocketUsers.getSessionByToken(token);
                    if (session != null) {
                        try {
                            WebSocketUsers.sendMessageToUserByText(session, objectMapper.writeValueAsString(SocketMessage.success("文档分段向量化处理完成")));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        });

        Map<String, Object> res = MapUtil.<String, Object>builder()
                .put("batchSize", batchSize)
                .put("segmentSize", segments.size())
                .build();
        return success(res);
    }


    @GetMapping("/segment-query")
    @PreAuthorize("@ss.hasPermi('ai:knowledgeBase:list')")
    public TableDataInfo segmentQuery(@NotBlank @Param("kbId") Long kbId, @Param("pageNum") int pageNum,
                                      @Param("pageSize") int pageSize) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(Constants.KB_ID, kbId);
        List<Map<String, Object>> all = langChain4jService.querySegmentTextEqualsByMetaData(
                metadata).stream().map(x -> {
            Map<String, Object> map = new HashMap<>();
            map.put("embeddingId", x.get("embeddingId"));
            map.put("text", x.get("text"));
            return map;
        }).collect(Collectors.toList());
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = startIndex + pageSize;
        TableDataInfo tableDataInfo = new TableDataInfo();
        int total = all.size();
        tableDataInfo.setTotal(total);
        tableDataInfo.setCode(HttpStatus.SUCCESS);
        tableDataInfo.setRows(CollUtil.sub(all, startIndex, endIndex));
        return tableDataInfo;
    }

    @GetMapping("/segment-query/{id}")
    @PreAuthorize("@ss.hasPermi('ai:knowledgeBase:list')")
    public AjaxResult getSegment(@PathVariable("id") String id) {
        Map<String, Object> segment = pgVectorUtil.selectById(id);
        segment.remove("metadata");
        segment.remove("embedding");
        return success(segment);
    }

    @DeleteMapping("/segment-del/{ids}")
    @PreAuthorize("@ss.hasPermi('ai:knowledgeBase:list')")
    public AjaxResult delSegment(@PathVariable String[] ids) {
        langChain4jService.removeSegment(CollUtil.toList(ids));
        return success();
    }

    @PutMapping("/segment-update")
    @PreAuthorize("@ss.hasPermi('ai:knowledgeBase:list')")
    public AjaxResult updateSegment(@RequestBody TextEmbeddingReq textEmbeddingReq) {
        Long embeddingModelId = textEmbeddingReq.getEmbeddingModelId();
        Model model = modelService.selectModelById(embeddingModelId);
        EmbeddingModel embeddingModel = modelBuilder.getEmbeddingModel(model);
        Metadata metadata = new Metadata();
        metadata.put(Constants.KB_ID, textEmbeddingReq.getKbId());
        metadata.put("update_ts", LocalDateTime.now().toString());
        metadata.put("update_by", getUsername());
        TextSegment textSegment = TextSegment.from(textEmbeddingReq.getText(), metadata);
        langChain4jService.updateSegment(embeddingModel, textSegment,
                textEmbeddingReq.getEmbeddingId());
        return success();
    }
}
