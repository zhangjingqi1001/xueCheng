package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 选课相关操作
 */
@Slf4j
@Service
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;

    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;

    @Autowired
    ContentServiceClient contentServiceClient;

    @Transactional
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        // 1.Feign远程调用内容管理服务查询课程收费规则（从发布表中查询对应的课程是收费还是免费）
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            XueChengPlusException.cast("课程不存在");
        }
        // 课程收费规则（是否收费）
        String charge = coursepublish.getCharge();
        // 选课记录
        XcChooseCourse chooseCourse = null;
        // 2.免费课程：向选课记录表、我的课程表中写入数据(课程表的数据来源于选课记录表)
        if ("201000".equals(charge)) {
            // 向选课记录表中写入
            chooseCourse = addFreeCourse(userId, coursepublish);
            // 向课程表中写入
            XcCourseTables xcCourseTables = addCourseTables(chooseCourse);
        } else {
            // 3.收费课程：向炫酷记录表写入数据，等待用户支付完成后再向课程表中写入数据
            // 此模块不会向课程表中添加记录了
            chooseCourse = addChargeCourse(userId, coursepublish);
        }

        // 4.判断学生目前对此课程是否具有学习资格，并且要将此学习资格返回
        XcCourseTablesDto courseTablesDto = getLearningStatus(userId, courseId);
        // 构造返回值
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse, xcChooseCourseDto);
        xcChooseCourseDto.setLearnStatus(courseTablesDto.getLearnStatus());
        return xcChooseCourseDto;
    }

    //添加免费课程,免费课程加入选课记录表
    public XcChooseCourse addFreeCourse(String userId, CoursePublish coursepublish) {
        // 不一定是添加，因为可能会有人多次点击“添加课程/学习课程”之类的按钮
        // 如果此课程已经被此用户选择了且选课的状态为成功，那就不允许用户再选择，直接返回结果即可
        LambdaQueryWrapper<XcChooseCourse> lqw = new LambdaQueryWrapper<>();
        // 哪一位用户
        lqw.eq(XcChooseCourse::getUserId, userId)
                // 课程id
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                // 课程类型为免费课程
                .eq(XcChooseCourse::getOrderType, "700001")
                // 选课成功
                .eq(XcChooseCourse::getStatus, "701001");
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(lqw);
        if (xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);
        }
        // 运行到这里说明数据库中没有对应的选课记录，添加一份即可
        XcChooseCourse chooseCourse = new XcChooseCourse();
        chooseCourse.setCourseId(coursepublish.getId()); //课程id
        chooseCourse.setCourseName(coursepublish.getName()); //课程名称
        chooseCourse.setCoursePrice(coursepublish.getPrice());//免费课程价格为0
        chooseCourse.setUserId(userId); //用户名
        chooseCourse.setCompanyId(coursepublish.getCompanyId());//机构id
        chooseCourse.setOrderType("700001");//免费课程代码标识
        chooseCourse.setCreateDate(LocalDateTime.now()); //创建时间
        chooseCourse.setStatus("701001");//选课成功，选课状态标识

        chooseCourse.setValidDays(365);//免费课程默认365
        chooseCourse.setValidtimeStart(LocalDateTime.now());// 课程开始时间
        chooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365)); //课程结束时间
        int insert = xcChooseCourseMapper.insert(chooseCourse);
        if (insert <= 0) {
            XueChengPlusException.cast("添加课程失败");
        }
        return chooseCourse;
    }

    //添加收费课程
    public XcChooseCourse addChargeCourse(String userId, CoursePublish coursepublish) {
        // 不一定是添加，因为可能会有人多次点击“添加课程/学习课程”之类的按钮
        // 查询选课表中，是否有此收费课程在选课记录表中的选课状态为待支付
        LambdaQueryWrapper<XcChooseCourse> lqw = new LambdaQueryWrapper<>();
        // 哪一位用户
        lqw.eq(XcChooseCourse::getUserId, userId)
                // 课程id
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                // 课程类型为收费课程
                .eq(XcChooseCourse::getOrderType, "700002")
                // 状态不是选课成功，而是待支付
                .eq(XcChooseCourse::getStatus, "701002");
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(lqw);
        if (xcChooseCourses.size() > 0) {
            return xcChooseCourses.get(0);
        }
        // 运行到这里说明数据库中没有对应的选课记录，添加一份即可
        XcChooseCourse chooseCourse = new XcChooseCourse();
        chooseCourse.setCourseId(coursepublish.getId()); //课程id
        chooseCourse.setCourseName(coursepublish.getName()); //课程名称
        chooseCourse.setCoursePrice(coursepublish.getPrice());//免费课程价格为0
        chooseCourse.setUserId(userId); //用户名
        chooseCourse.setCompanyId(coursepublish.getCompanyId());//机构id
        chooseCourse.setOrderType("700002");//收费课程代码标识
        chooseCourse.setCreateDate(LocalDateTime.now()); //创建时间
        chooseCourse.setStatus("701002");//选课成功，选课状态标识

        chooseCourse.setValidDays(365);//免费课程默认365
        chooseCourse.setValidtimeStart(LocalDateTime.now());// 课程开始时间
        chooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365)); //课程结束时间
        int insert = xcChooseCourseMapper.insert(chooseCourse);
        if (insert <= 0) {
            XueChengPlusException.cast("添加课程失败");
        }
        return chooseCourse;
    }

    //添加到我的课程表（同一个人同一门课只会有同一条记录,因为这里我们已经在数据库添加约束了）
    public XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse) {
        //选课记录完成且未过期可以添加课程到课程表
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)) {
            // 701001代表选课完成，其他状态都代表未完成
            XueChengPlusException.cast("选课未成功，无法添加到课程表");
        }
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if (xcCourseTables != null) {
            // 说明课程已经在课程表中了
            return xcCourseTables;
        }
        xcCourseTables = new XcCourseTables();
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId()); // 选课表中的主键
        xcCourseTables.setUserId(xcChooseCourse.getUserId());
        xcCourseTables.setCourseId(xcChooseCourse.getCourseId());
        xcCourseTables.setCompanyId(xcChooseCourse.getCompanyId());
        xcCourseTables.setCourseName(xcChooseCourse.getCourseName());
        xcCourseTables.setCreateDate(LocalDateTime.now());
        xcCourseTables.setValidtimeStart(xcChooseCourse.getValidtimeStart());
        xcCourseTables.setValidtimeEnd(xcChooseCourse.getValidtimeEnd());
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());
        int insert = xcCourseTablesMapper.insert(xcCourseTables);
        if (insert <= 0) {
            XueChengPlusException.cast("课程添加到课程表失败");
        }
        return xcCourseTables;

    }

    /**
     * @param userId
     * @param courseId
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @description 根据课程和用户查询我的课程表中某一门课程
     */
    public XcCourseTables getXcCourseTables(String userId, Long courseId) {
        LambdaQueryWrapper<XcCourseTables> lqw = new LambdaQueryWrapper<>();
        lqw.eq(XcCourseTables::getUserId, userId)
                .eq(XcCourseTables::getCourseId, courseId);
        return xcCourseTablesMapper.selectOne(lqw);
    }

    /**
     * 查询课程表
     *
     * @param userId
     * @param courseId
     * @return XcCourseTablesDto 学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     * @description 判断学习资格
     */
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        // 查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if (xcCourseTables == null) {
            // 如果查不到，说明没有选课或者选课后未支付
            XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }

        //如果有记录，判断是否过期，如果过期了就不能学习，如果没过期可以正常学习
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        BeanUtils.copyProperties(xcCourseTables, xcCourseTablesDto);
        //是否过期,true过期，false未过期
        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if (!isExpires) {
            //正常学习
            xcCourseTablesDto.setLearnStatus("702001");
            return xcCourseTablesDto;
        } else {
            //已过期
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }
    }
}
