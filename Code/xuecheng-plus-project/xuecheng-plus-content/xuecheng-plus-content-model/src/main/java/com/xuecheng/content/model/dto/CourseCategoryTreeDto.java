package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseCategory;
import lombok.Data;

import java.io.Serializable;
import java.util.List;


@Data
public class CourseCategoryTreeDto extends CourseCategory implements Serializable {

    //Serializable:在网络传输需要序列化的时候，需要实现Serializable接口
    private static final long serialVersionUID = 2950235607890841126L;

    //下级节点
    List<CourseCategoryTreeDto> childrenTreeNodes;

}
