package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


/**
 * 课程信息管理业务接口实现类
 */
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(Long companyId,PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {


        //构建查询条件对象
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //构建查询条件，根据课程名称查询
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()), CourseBase::getName, queryCourseParamsDto.getCourseName());
        //构建查询条件，根据课程审核状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()), CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());
        //构建查询条件，根据课程发布状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()), CourseBase::getStatus, queryCourseParamsDto.getPublishStatus());
        //TODO 根据培训机构的id拼装查询条件
        queryWrapper.eq(StringUtils.isNotEmpty(companyId.toString()), CourseBase::getCompanyId, companyId);

        //分页对象
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<CourseBase> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<CourseBase> courseBasePageResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());

        return courseBasePageResult;

    }


    /**
     * 新增课程
     *
     * @param companyId 教学机构id （新增课程操作的教学机构）
     * @param dto       课程基本信息
     * @return CourseBaseInfoDto 课程添加成功后返回课程详细信息
     * @description 添加课程基本信息
     */
    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        //TODO 将来会集成SpringSecurity框架，用户登录之后就可以获取到用户所属机构的ID
        //我们这个地方companyId要先写死

        //TODO 向课程基本信息表course_base写入数据
        CourseBase courseBaseNew = new CourseBase();
        //将填写的课程信息赋值给新增对象
        BeanUtils.copyProperties(dto, courseBaseNew);//拷贝的前提是属性名一致
        //下面是手动设置一些值
        //设置审核状态
        courseBaseNew.setAuditStatus("202002");//未审核
        //设置发布状态
        courseBaseNew.setStatus("203001");//未发布
        //机构id
        courseBaseNew.setCompanyId(companyId);
        //添加时间
        courseBaseNew.setCreateDate(LocalDateTime.now());
        //插入课程基本信息表
        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert <= 0) {
            throw new RuntimeException("新增课程基本信息失败");
        }

        //TODO 向课程营销表course_market写入数据
        CourseMarket courseMarketNew = new CourseMarket();
        //设置主键,和课程基本信息表的id相同
        Long courseId = courseBaseNew.getId();
        //将页面输入的信息拷贝到courseMarketNew对象
        BeanUtils.copyProperties(dto, courseMarketNew);
        courseMarketNew.setId(courseId);
        //保存
        int i = saveCourseMarket(courseMarketNew);
        if (i <= 0) {
            throw new RuntimeException("保存课程营销信息失败");
        }

        //TODO 查询课程基本信息及营销信息并返回
        return getCourseBaseInfo(courseId);

    }

    /**
     * 保存营销信息
     * 逻辑：存在则更新对应的营销信息，不存在则添加对应的营销信息
     *
     * @param courseMarketNew 要保存的营销信息
     * @return 是否添加/修改成功
     */
    private int saveCourseMarket(CourseMarket courseMarketNew) {
        //收费规则
        String charge = courseMarketNew.getCharge();
        if (StringUtils.isBlank(charge)) {
            throw new RuntimeException("收费规则没有选择");
        }
        //收费规则为收费
        if (charge.equals("201001")) {
            if (courseMarketNew.getPrice() == null || courseMarketNew.getPrice().floatValue() <= 0) {
                throw new RuntimeException("课程为收费价格不能为空且必须大于0");
            }
        }
        //根据id从课程营销表查询
        CourseMarket courseMarketObj = courseMarketMapper.selectById(courseMarketNew.getId());
        if (courseMarketObj == null) {
            //说明不存在对应的营销信息，直接添加
            return courseMarketMapper.insert(courseMarketNew);
        } else {
            //存在对应的营销信息，修改营销信息
            BeanUtils.copyProperties(courseMarketNew, courseMarketObj);
            courseMarketObj.setId(courseMarketNew.getId());
            return courseMarketMapper.updateById(courseMarketObj);
        }
    }

    /**
     * 从数据库查询课程的详细信息
     *
     * @param courseId 课程id
     * @return 课程的详细信息，包括课程基本信息和课程营销信息
     */
    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {

        //TODO 查询课程基本信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            return null;
        }
        //TODO 查询课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }

        //TODO 查询分类名称，是哪一级的
        CourseCategory courseCategoryBySt = courseCategoryMapper.selectById(courseBase.getSt());//小分类
        courseBaseInfoDto.setStName(courseCategoryBySt.getName());//小分类名称

        CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());//大分类
        courseBaseInfoDto.setMtName(courseCategoryByMt.getName());//大分类名称

        return courseBaseInfoDto;

    }

    /**
     * 修改课程
     *
     * @param companyId     机构id，后面做认证收取那使用
     * @param editCourseDto 要修改的课程信息
     * @return 修改之后的课程详细信息
     */
    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        //TODO 课程基本信息
        //获取到课程id
        Long courseId = editCourseDto.getId();
        //查询课程
        CourseBase courseBase = courseBaseMapper.selectById(courseId);

        if (courseBase == null) {
            XueChengPlusException.cast("课程不存在");
        }
        //数据合法性校验
        //根据具体的业务逻辑进行校验 - 本机构只能修改本机构的课程
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengPlusException.cast("本机构只能修改本机构的课程");
        }

        //封装数据
        BeanUtils.copyProperties(editCourseDto, courseBase);
        //修改时间
        courseBase.setChangeDate(LocalDateTime.now());
        //更新数据库
        int i = courseBaseMapper.updateById(courseBase);
        if (i<=0){
            XueChengPlusException.cast("修改课程失败");
        }

        //TODO 课程营销信息
        CourseMarket courseMarketNew = new CourseMarket();
        BeanUtils.copyProperties(editCourseDto, courseMarketNew);
        int count = courseMarketMapper.updateById(courseMarketNew);
        if (count <= 0) {
            throw new RuntimeException("更新课程营销信息失败");
        }

        //查询课程信息
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);

        return courseBaseInfo;
    }

}
