package com.xuecheng.content.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程内容管理
 */
@Api(value = "课程信息编辑接口",tags = "课程信息编辑接口")
@Slf4j
@RequestMapping
@RestController //@Controller+@Response
public class CourseBaseInfoController {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    /**
     * 课程分页查询接口
     *
     * @RequestBody(required = false) 含义：不传QueryCourseParamsDto请求体也行
     *
     * @param pageParams           pageNo字段和pageSize字段
     * @param queryCourseParamsDto auditStatus审核状态字段，courseName课程名称字段，publishStatus发布状态字段
     * @return 分页结果
     */
    @ApiOperation("课程查询接口")
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto) {
        PageResult<CourseBase> pageResult = courseBaseInfoService.queryCourseBaseList(pageParams, queryCourseParamsDto);
        return pageResult;

    }

}
