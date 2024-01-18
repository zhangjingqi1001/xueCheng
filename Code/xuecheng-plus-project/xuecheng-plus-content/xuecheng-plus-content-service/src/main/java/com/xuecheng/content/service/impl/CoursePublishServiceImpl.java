package com.xuecheng.content.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableId;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程发布相关业务
 */
@Service
public class CoursePublishServiceImpl implements CoursePublishService {

    //课程基础信息（课程营销信息表/课程基本信息表）
    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    //课程计划
    @Autowired
    TeachplanService teachplanService;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    CoursePublishMapper coursePublishMapper;

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CoursePublishServiceImpl coursePublishService;

    @Autowired
    MqMessageService mqMessageService;


    /**
     * @param courseId 课程id
     * @description 获取课程预览信息
     */
    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        // 从数据库查询模型的数据（课程营销信息表、课程师资表、课程基本信息表、课程计划）
        // 课程基本信息，营销信息
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.getCourseBaseInfo(courseId);

        //课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);

        // 组装返回信息
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfoDto);
        coursePreviewDto.setTeachplans(teachplanTree);
        return coursePreviewDto;
    }

    /**
     * @param courseId 课程id
     * @description 提交审核
     */
    @Transactional
    @Override
    public void commitAudit(Long companyId, Long courseId) {
        // 课程基本信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        if (courseBaseInfo == null) {
            XueChengPlusException.cast("课程找不到");
        }
        //得到审核状态
        String auditStatus = courseBaseInfo.getAuditStatus();
        //TODO 如果课程的审核状态为已提交则不允许提交
        if ("202003".equals(auditStatus)) {
            XueChengPlusException.cast("课程已经提交申请，请您耐心等待审核");
        }

        //TODO 本机构只能提交本机构的课程

        //TODO 如果课程的图片、计划信息没有填写不允许提交
        if (StringUtils.isEmpty(courseBaseInfo.getPic())) {
            XueChengPlusException.cast("请上传课程图片");
        }
        //查询课程计划
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(courseId);

        if (coursePreviewInfo == null || coursePreviewInfo.getTeachplans().size() == 0) {
            XueChengPlusException.cast("请上传课程计划");
        }

        //TODO 1.查询到课程基本信息、营销信息、计划等信息插入到课程预报布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();

        //TODO 1.1课程基本信息加部分营销信息
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);
        // 设置机构id
        coursePublishPre.setCompanyId(companyId);
        //课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //转为json
        String courseMarketJson = JSON.toJSONString(courseMarket);
        //将课程营销信息json数据放入课程预发布表
        coursePublishPre.setMarket(courseMarketJson);

        //TODO 1.2查询课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if (teachplanTree.size() <= 0) {
            XueChengPlusException.cast("提交失败，还没有添加课程计划");
        }
        //转json
        String teachplanTreeString = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanTreeString);

        //TODO 1.3设置预发布记录状态,已提交
        coursePublishPre.setStatus("202003");
        //教学机构id
        coursePublishPre.setCompanyId(companyId);
        //提交时间
        coursePublishPre.setCreateDate(LocalDateTime.now());
        CoursePublishPre coursePublishPreUpdate = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPreUpdate == null) {
            //添加课程预发布记录
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        //TODO 2.更新课程基本信息表的审核状态为已提交
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        // 审核状态为已提交
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);
    }

    /**
     * 课程发布接口
     *
     * @param companyId 机构id
     * @param courseId  课程id
     */
    @Transactional
    @Override
    public void publish(Long companyId, Long courseId) {

        //TODO 1.查询预发布表，课程如果没有审核通过不允许发布，通过了向课程发布表写数据
        //1.1 查询预发布表
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);

        if (coursePublishPre == null) {
            XueChengPlusException.cast("课程无审核记录，无法发布");
        }

        //预发布表的审核状态（审核通过202004）
        String status = coursePublishPre.getStatus();
        if (!"202004".equals(status)) {
            //课程没有审核通过不允许发布
            XueChengPlusException.cast("课程没有审核通过不允许发布");
        }
        //1.2 向课程发布表写入数据
        CoursePublish coursePublish = new CoursePublish();
        //课程预发布表和课程发布表ode数据结构是一个样子的
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        //首先查询课程发布表，看看此课程之前是是否发布过
        CoursePublish coursePublishOld = coursePublishMapper.selectById(courseId);
        if (coursePublishOld == null) {
            coursePublishMapper.insert(coursePublish);
        } else {
            coursePublishMapper.updateById(coursePublishOld);
        }

        //TODO 2.向消息表写入数据
        this.saveCoursePublishMessage(courseId);

        //TODO 3.将预发布表的数据删除
        coursePublishPreMapper.deleteById(courseId);
    }

    /**
     * 保存消息表记录
     *
     * @param courseId 课程id
     */
    private void saveCoursePublishMessage(Long courseId) {
        //我们可以任务约定一下课程发布的messageType是course_publish
        //剩下三个参数是业务信息字段,如果不使用的话填空即可
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", courseId.toString(), null, null);
        if (mqMessage == null) {
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }

    }

}
