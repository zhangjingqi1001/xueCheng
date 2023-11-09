package com.xuecheng.content.api;

import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.model.po.CourseBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class ContentApplicationTest {

    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Test
    public void test() {
        CourseBase courseBase = courseBaseMapper.selectById(1);
        System.out.println(courseBase);

    }

    @Autowired
    private CourseCategoryMapper courseCategoryMapper;

    @Test
    public void test02(){
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes("1");
        System.out.println(courseCategoryTreeDtos);
    }


}
