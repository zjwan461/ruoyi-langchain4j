package com.ruoyi.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.ai.config.AiConfig;
import com.ruoyi.ai.domain.AgentSkill;
import com.ruoyi.ai.mapper.AgentSkillMapper;
import com.ruoyi.ai.service.IAgentSkillService;
import com.ruoyi.common.utils.DateUtils;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skill;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.file.Path;
import java.util.List;

/**
 * agent_skillService业务层处理
 *
 * @author Jerry
 * @date 2026-07-13
 */
@Service
public class AgentSkillServiceImpl implements IAgentSkillService, InitializingBean {
    @Autowired
    private AgentSkillMapper agentSkillMapper;

    @Resource
    private AiConfig aiConfig;

    /**
     * 查询agent_skill
     *
     * @param id agent_skill主键
     * @return agent_skill
     */
    @Override
    public AgentSkill selectAgentSkillById(Long id) {
        return agentSkillMapper.selectAgentSkillById(id);
    }

    /**
     * 查询agent_skill列表
     *
     * @param agentSkill agent_skill
     * @return agent_skill
     */
    @Override
    public List<AgentSkill> selectAgentSkillList(AgentSkill agentSkill) {
        return agentSkillMapper.selectAgentSkillList(agentSkill);
    }

    /**
     * 新增agent_skill
     *
     * @param agentSkill agent_skill
     * @return 结果
     */
    @Override
    public int insertAgentSkill(AgentSkill agentSkill) {
        agentSkill.setCreateTime(DateUtils.getNowDate());
        return agentSkillMapper.insertAgentSkill(agentSkill);
    }

    /**
     * 修改agent_skill
     *
     * @param agentSkill agent_skill
     * @return 结果
     */
    @Override
    public int updateAgentSkill(AgentSkill agentSkill) {
        agentSkill.setUpdateTime(DateUtils.getNowDate());
        return agentSkillMapper.updateAgentSkill(agentSkill);
    }

    /**
     * 批量删除agent_skill
     *
     * @param ids 需要删除的agent_skill主键
     * @return 结果
     */
    @Override
    public int deleteAgentSkillByIds(Long[] ids) {
        return agentSkillMapper.deleteAgentSkillByIds(ids);
    }

    /**
     * 删除agent_skill信息
     *
     * @param id agent_skill主键
     * @return 结果
     */
    @Override
    public int deleteAgentSkillById(Long id) {
        return agentSkillMapper.deleteAgentSkillById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void afterPropertiesSet() throws Exception {
        Path localSkillPath = aiConfig.getSkill().getLocalSkillPath();
        if (aiConfig.getSkill().isSync2Db()) {
            syncSkills2Db(localSkillPath);
        }
    }

    private void syncSkills2Db(Path localSkillPath) {
        List<FileSystemSkill> skills = FileSystemSkillLoader.loadSkills(localSkillPath);
        if (CollUtil.isNotEmpty(skills)) {
            List<String> skillNames = skills.stream().map(Skill::name).toList();
            agentSkillMapper.deleteAgentSkillByNames(skillNames);
            List<AgentSkill> agentSkills = skills.stream().map(x -> {
                AgentSkill agentSkill = new AgentSkill();
                agentSkill.setCreateTime(DateUtils.getNowDate());
                agentSkill.setSkillName(x.name());
                agentSkill.setSkillContent(x.content());
                agentSkill.setSkillDescription(x.description());
                return agentSkill;
            }).toList();
            agentSkillMapper.batchInsertAgentSkill(agentSkills);
        }
    }
}
