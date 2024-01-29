package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    XcMenuMapper xcMenuMapper;

    //注入，将来查询对象
    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    ApplicationContext applicationContext;

    /**
     * @param s 传入的请求认证的参数是AuthParamDtoJSON串，此时不再是输入的username（账号）
     */
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        // 1.将传入的JSON转成AuthParamsDto对象
        AuthParamsDto authParamsDto = null;
        try {
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("请求认证的参数不符合要求");
        }

        // 2.判断登录的类型（认证类型,有password、wx）
        String authType = authParamsDto.getAuthType();
        String beanName = authType + "_authservice";
        // 取出指定的bean
        AuthService authService = applicationContext.getBean(beanName, AuthService.class);

        // 3.调用统一认证方法完成认证
        XcUserExt xcUserExt = authService.execute(authParamsDto);

        // 4.封装XcUserExt为UserDetails类型
        UserDetails userDetails = this.getUserPrincipal(xcUserExt);
        return userDetails;
    }

    /**
     * 将XcUserExt数据封装成UserDetails数据
     *
     * @param xcUserExt 用户id，主键
     * @return com.xuecheng.ucenter.model.po.XcUser 用户信息
     * @description 查询用户信息
     */
    public UserDetails getUserPrincipal(XcUserExt xcUserExt) {
        // 权限信息
        String[] authorities = {"p1"};

        String password = xcUserExt.getPassword();
        //用户权限,如果不加报Cannot pass a null GrantedAuthority collection
        //TODO 根据用户id查询用户权限
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(xcUserExt.getId());
        if (xcMenus.size() > 0) {
            List<String> permissions = new ArrayList<>();
            xcMenus.forEach(m -> {
                // 拿到用户拥有的权限标识符
                permissions.add(m.getCode());
            });
            //permissions 转成数组
            authorities = permissions.toArray(new String[0]);
        }
        xcUserExt.setPassword(""); // 为了安全考虑，专JSON前吧密码做空
        String userString = JSON.toJSONString(xcUserExt);
        UserDetails userDetails = User.withUsername(userString)
                //用户密码
                .password(password)
                //用户权限,暂时先不写
                .authorities(authorities).build();
        return userDetails;
    }

}
