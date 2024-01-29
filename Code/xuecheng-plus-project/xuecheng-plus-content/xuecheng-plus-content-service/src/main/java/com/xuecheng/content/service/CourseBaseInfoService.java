package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;

public interface CourseBaseInfoService {

    PageResult<CourseBase> queryCourseBaseList(Long companyId,PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

    /**
     * 新增课程
     *
     * @param companyId    教学机构id （新增课程操作的教学机构）
     * @param addCourseDto 课程基本信息
     * @return CourseBaseInfoDto 课程添加成功后返回课程详细信息
     * @description 添加课程基本信息
     */
    CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto);

    /**
     * 根据课程id查询课程信息
     * @param courseId 课程Id
     * @return 课程信息
     */
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId);


    /**
     * 修改课程
     * @param companyId 机构id，后面做认证收取那使用
     * @param editCourseDto 要修改的课程信息
     * @return 修改之后的课程详细信息
     */
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto);

}
