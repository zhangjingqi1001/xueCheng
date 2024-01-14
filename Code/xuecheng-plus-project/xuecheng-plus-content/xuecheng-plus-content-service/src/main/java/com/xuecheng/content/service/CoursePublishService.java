package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;

/**
 * 课程发布相关接口
 */
public interface CoursePublishService {
    /**
     * @description 获取课程预览信息
     * @param courseId 课程id
     */
    public CoursePreviewDto getCoursePreviewInfo(Long courseId);

    /**
     * @description 提交审核
     * @param courseId  课程id
     */
    public void commitAudit(Long companyId,Long courseId);

}
