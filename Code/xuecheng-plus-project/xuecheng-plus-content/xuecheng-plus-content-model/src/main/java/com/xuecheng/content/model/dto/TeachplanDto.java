package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
public class TeachplanDto extends Teachplan {
    private static final long serialVersionUID = 1586804545070538974L;

    //课程计划关联的媒资信息
    private TeachplanMedia teachplanMedia;

    //子节点
    private List<TeachplanDto> teachPlanTreeNodes;

}
