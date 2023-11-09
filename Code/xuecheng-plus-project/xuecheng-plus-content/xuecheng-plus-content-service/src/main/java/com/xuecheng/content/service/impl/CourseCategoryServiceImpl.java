package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    private CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //TODO 数据库递归查询出课程分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);


        //TODO 找到每个节点的子节点，最终封装成List<CourseCategoryTreeDto>
        //将list转map,以备使用,排除根节点
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream()
                //!id.equals(item.getId()) 含义就是排除根节点
                .filter(item -> !id.equals(item.getId()))
                .collect(
                        //转Map是需要一个key，一个value的
                        //第一个key是代表元素的意思，key -> key.getId()是拿到key元素的id，然后充当Map的key
                        //value表示对象的本身，所以不需要任何的处理
                        //(key1, key2) -> key2 表示当key重复的时候（键相同），以后来的key为主
                        Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2)
                );

        //最终返回的list
        List<CourseCategoryTreeDto> categoryTreeDtos = new ArrayList<>();

        //依次遍历每个元素,排除根节点
        //courseCategoryTreeDtos是从数据库查询出来的全部的数据
        courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).forEach(item -> {
            if (item.getParentid().equals(id)) {
                //紧挨根节点下的节点
                categoryTreeDtos.add(item);
            }
            //找到当前节点的父节点
            CourseCategoryTreeDto courseCategoryTreeDto = mapTemp.get(item.getParentid());
            if (courseCategoryTreeDto != null) {
                if (courseCategoryTreeDto.getChildrenTreeNodes() == null) {
                    courseCategoryTreeDto.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                //下边开始往ChildrenTreeNodes属性中放子节点
                courseCategoryTreeDto.getChildrenTreeNodes().add(item);
            }
        });
        return categoryTreeDtos;
    }

}
