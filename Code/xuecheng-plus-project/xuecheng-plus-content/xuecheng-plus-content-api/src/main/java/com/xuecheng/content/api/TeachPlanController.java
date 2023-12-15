package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.TeachplanDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 课程计划相关接口
 */
@Api(value = "课程计划编辑接口",tags = "课程计划编辑接口")
@RestController
public class TeachPlanController {

    //查询课程计划
    @ApiOperation("查询课程计划树形结构")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    //针对参数进行说明
    @ApiImplicitParam(value = "courseId",name = "课程id",required = true,dataType = "Long",paramType = "path")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId) {
        return null;
    }
}
