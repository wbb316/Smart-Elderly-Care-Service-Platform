package com.lcyl.code.controller;

import com.lcyl.common.core.domain.AjaxResult;
import org.activiti.engine.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程部署控制器
 */
@RestController
@RequestMapping("/process")
public class ProcessDeployController {

    @Autowired
    private RepositoryService repositoryService;

    /**
     * 部署请假流程
     */
    @PostMapping("/deploy/qingjia")
    public AjaxResult deployQingjiaProcess() {
        try {
            repositoryService.createDeployment()
                    .name("请假工作流")
                    .addClasspathResource("processes/qingjia.bpmn")
                    .deploy();
            return AjaxResult.success("请假流程部署成功");
        } catch (Exception e) {
            return AjaxResult.error("请假流程部署失败：" + e.getMessage());
        }
    }
}
