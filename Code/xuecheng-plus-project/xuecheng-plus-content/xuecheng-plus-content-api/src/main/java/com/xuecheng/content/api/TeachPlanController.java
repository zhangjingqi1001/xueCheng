package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.service.impl.TeachplanServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程计划相关接口
 */
@Api(value = "课程计划编辑接口",tags = "课程计划编辑接口")
@RestController
public class TeachPlanController {

    @Autowired
    private TeachplanServiceImpl teachplanService;

    //查询课程计划
    @ApiOperation("查询课程计划树形结构")
    @GetMapping("/teachplan/{courseId}/tree-nodes")
    //针对参数进行说明
    @ApiImplicitParam(value = "courseId",name = "课程id",required = true,dataType = "Long",paramType = "path")
    public List<TeachplanDto> getTreeNodes(@PathVariable Long courseId) {
        return teachplanService.findTeachplanTree(courseId);
    }

    @ApiOperation("课程计划创建或修改")
    @PostMapping("/teachplan")
    public void saveTeachplan(@RequestBody SaveTeachplanDto teachplanDto){
        teachplanService.saveTeachplan(teachplanDto);
    }

}
