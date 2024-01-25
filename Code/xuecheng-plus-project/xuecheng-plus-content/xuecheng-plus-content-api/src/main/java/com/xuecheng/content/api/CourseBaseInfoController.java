package com.xuecheng.content.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.utils.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 课程内容管理
 */
@Api(value = "课程信息编辑接口", tags = "课程信息编辑接口")
@Slf4j
@RequestMapping
@RestController //@Controller+@Response
public class CourseBaseInfoController {

    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    /**
     * 课程分页查询接口
     *
     * @param pageParams           pageNo字段和pageSize字段
     * @param queryCourseParamsDto auditStatus审核状态字段，courseName课程名称字段，publishStatus发布状态字段
     * @return 分页结果
     * @RequestBody(required = false) 含义：不传QueryCourseParamsDto请求体也行
     */
    @ApiOperation("课程分页查询接口")
    @PostMapping("/course/list")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required = false) QueryCourseParamsDto queryCourseParamsDto) {
        PageResult<CourseBase> pageResult = courseBaseInfoService.queryCourseBaseList(pageParams, queryCourseParamsDto);
        return pageResult;

    }

    @ApiOperation("新增课程")
    @PostMapping("/course")
    public CourseBaseInfoDto createCourseBase(@Validated @RequestBody AddCourseDto addCourseDto) {
//      将来会集成SpringSecurity框架，用户登录之后就可以获取到用户所属机构的ID
//      先把机构ID写死
        return courseBaseInfoService.createCourseBase(10086L, addCourseDto);
    }

    @ApiOperation("根据课程id查询接口")
    @GetMapping("/course/{courseId}")
    public CourseBaseInfoDto getCourseBaseById(@PathVariable("courseId") Long courseId) {
        return courseBaseInfoService.getCourseBaseInfo(courseId);
    }

    @ApiOperation("修改课程接口")
    @PutMapping("/course")
    public CourseBaseInfoDto getCourseBaseById(@RequestBody EditCourseDto editCourseDto) {
        // getContext() 获取上下文对象,getAuthentication()拿到认证信息,getPrincipal()拿到身份信息
        //SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        System.out.println(user.getUsername());
        //机构id先写死，后面授权认证的时候后会改过来
        Long companyId = 1232141425L;
        return courseBaseInfoService.updateCourseBase(companyId, editCourseDto);
    }

}
