package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {

    @Autowired
    private TeachplanMapper teachplanMapper;

    /**
     * 查询课程计划树形结构
     *
     * @param courseId 课程id
     * @return 课程计划树形结构
     */
    @Override
    public List<TeachplanDto> findTeachplanTree(long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }


    private int getTeachplanCount(long courseId, long parentId) {

        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId, courseId)
                .eq(Teachplan::getParentid, parentId);
        return teachplanMapper.selectCount(queryWrapper);
    }

    /**
     * 新增/修改/保存课程计划
     *
     * @param saveTeachplanDto
     */
    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        //通过课程计划id判断是新增还是修改（新增新增课程的时候不会有id）
        //教学计划id
        Long teachplanId = saveTeachplanDto.getId();
        if (teachplanId == null) {
            //新增
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);
            //确定排序的字段
            // 大章节的添加： 查询某个课程id下有多少个大章节即可
            // select count(1) from teachplan where course_id = 117 and parentid = 0
            // 小章节的添加：（我们只需要找到同级节点有几个即可，也就是parentId相同的就是同级的）
            //select count(1) from teachplan where course_id = 117 and parentid = 父级id
            Long parentId = saveTeachplanDto.getParentid();
            Long courseId = saveTeachplanDto.getCourseId();
            int count = this.getTeachplanCount(courseId, parentId);
            // 确定排序字段
            teachplan.setOrderby(count + 1);

            teachplanMapper.insert(teachplan);
        } else {
            //修改
            Teachplan teachplan = teachplanMapper.selectById(teachplanId);
            //将要修改的参数复制到teachplan里
            BeanUtils.copyProperties(saveTeachplanDto, teachplan);
            teachplanMapper.updateById(teachplan);
        }

    }
}
