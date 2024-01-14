package com.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

// 不要使用RestController，因为返回的是JSON数据，这里我们一定不要返回JSON数据
@Controller
public class FreemarkerController {
    /**
     *ModelAndView 模型和数据
     */
    @GetMapping("/testfreemarker")
    public ModelAndView test(){
        ModelAndView modelAndView = new ModelAndView();
        //指定模型数据
        modelAndView.addObject("name","小明");
        // 指定模型视图
        // 返回的是哪个视图，且不用加后缀.ftl（文件名test.ftl）
        // 因为我们已经在配置文件中告诉框架我们文件的后缀名称是什么了spring.freemarker.suffix=.ftl
        modelAndView.setViewName("test");
        return modelAndView;
    }
}
