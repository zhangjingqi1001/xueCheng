package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 课程发布的任务类
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    CoursePublishService coursePublishService;

    //任务调度入口
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();//第几个分片
        int shardTotal = XxlJobHelper.getShardTotal();//分片总数
        log.debug("shardIndex="+shardIndex+",shardTotal="+shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        //这个方法是MessageProcessAbstract抽象类的方法
        process(shardIndex,shardTotal,"course_publish",30,60);

    }

    /*
     * 执行课程发布的任务逻辑,在process方法中会调用抽象方法execute执行具体业务逻辑
     * MqMessage 数据库实体类
     * 如果此方法抛出异常说明任务执行失败
     */
    @Override
    public boolean execute(MqMessage mqMessage) {
        // 从mq_message拿到课程id
        // 认为规定的getBusinessKey1是courseId
        Long courseId = Long.parseLong(mqMessage.getBusinessKey1());

        //TODO 课程静态化上传minio
        this.generateCourseHtml(mqMessage, courseId);

        //TODO 向elasticsearch写索引数据
        this.saveCourseIndex(mqMessage, courseId);

        //TODO 向Redis写缓存


        //TODO 上面全部做完返回true
        return true;
    }

    // 实现课程静态化页面并上传至文件系统
    private void generateCourseHtml(MqMessage message, long courseId) {
        // 任务id（消息id）
        Long taskId = message.getId();
        // 通过get方法获取MqMessageService消息实体类
        MqMessageService mqMessageService = this.getMqMessageService();

        //TODO 做任务幂等性处理
        //查询数据库取出该阶段执行状态。每一个阶段的完成都会将相应的结果写入到相应的字段
        //这个地方其实就是取出的stageState1
        int stageOne = mqMessageService.getStageOne(taskId);
        if (stageOne > 0) {
            log.debug("课程静态化任务完成，无需处理");
            return;
        }

        //TODO 开始进行课程静态化
//        int i = 1 / 0;//制造一个异常表示任务执行中有问题
        // 1. 生成课程静态化页面
        File file = coursePublishService.generateCourseHtml(courseId);
        if (file == null){
            XueChengPlusException.cast("生成的静态页面为空");
        }
        // 2. 上传静态页面到文件系统
        coursePublishService.uploadCourseHtml(courseId,file);

        //TODO 任务处理完成写任务状态为完成
        //也就是stageState1字段的值是1
        mqMessageService.completedStageOne(taskId);
    }

    //保存课程索引信息
    public void saveCourseIndex(MqMessage mqMessage, long courseId) {
        // 任务id（消息id）
        Long taskId = mqMessage.getId();
        // 通过get方法获取MqMessageService消息实体类
        MqMessageService mqMessageService = this.getMqMessageService();

        //TODO 做任务幂等性处理
        //查询数据库取出该阶段执行状态。每一个阶段的完成都会将相应的结果写入到相应的字段
        //这个地方其实就是取出的stageState2
        int stageTwo = mqMessageService.getStageTwo(taskId);
        if (stageTwo > 0) {
            log.debug("课课程索引信息已写入，无需处理");
            return;
        }

        //TODO 查询课程信息，调用搜索服务添加索引

        //TODO 任务处理完成写任务状态为完成
        mqMessageService.completedStageTwo(taskId);
    }



}
