package com.xuecheng.content.api;

import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.po.CourseBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ContentApplicationTest {

    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Test
    public void test() {
        CourseBase courseBase = courseBaseMapper.selectById(1);
        System.out.println(courseBase);

    }


}
