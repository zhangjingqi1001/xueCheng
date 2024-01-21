package com.xuecheng.content;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class FreemarkerTest {
    @Autowired
    CoursePublishService coursePublishService;

    @Test
    public void testGenerateHtmlByTemplate() throws Exception {

        //TODO 准备模板
        // import freemarker.template.Configuration
        // new Configuration 实例时输入传入一下Configuration当前的版本
        Configuration configuration = new Configuration(Configuration.getVersion());
        // 找到模板路径
        String classPath = this.getClass().getResource("/").getPath();
        // 指定模板目录 (从哪个目录加载模板)
        configuration.setDirectoryForTemplateLoading(new File(classPath + "/templates"));
        // 指定编码
        configuration.setDefaultEncoding("UTF-8");
        // 得到模板
        Template template = configuration.getTemplate("course_template.ftl");

        //TODO 准备数据
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(120L);
        Map<String, Object> map = new HashMap<>();
        map.put("model", coursePreviewInfo);

        //TODO 将一个页面（源代码）转换成字符串
        // 参数1：模板  参数2：数据
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);

        //TODO 使用流将静态化内容输出到文件中
        InputStream inputStream = IOUtils.toInputStream(html,"utf-8");
        //输出流
        FileOutputStream outputStream = new FileOutputStream("D:\\1.html");
        IOUtils.copy(inputStream, outputStream);

    }
}
