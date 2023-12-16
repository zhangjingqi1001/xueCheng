package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

public interface TeachplanService {

    /**
     * 查询课程计划树形结构
     * @param courseId 课程id
     * @return 课程计划树形结构
     */
    public List<TeachplanDto> findTeachplanTree(long courseId);


    /**
     * 新增/修改/保存课程计划
     * @param saveTeachplanDto
     */
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto);

}
