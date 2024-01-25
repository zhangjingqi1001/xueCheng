package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.po.XcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UserServiceImpl implements UserDetailsService {

    //注入，将来查询对象
    @Autowired
    XcUserMapper xcUserMapper;

    /**
     * @param s 其实就是输入的username（账号）
     */
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        // 1.根据username账号查询数据库
        // 因为账号是不可能重复的，直接selectOne即可
        LambdaQueryWrapper<XcUser> lqw = new LambdaQueryWrapper<>();
        lqw.eq(XcUser::getUsername, s);
        XcUser xcUser = xcUserMapper.selectOne(lqw);

        // 2.查询不到用户，返回null即可，SpringSecurity框架会自动抛出异常“用户不存在”
        if (xcUser == null) {
            return null;
        }
        // 3.如果查到了用户并查询到正确的密码，将用户信息封装成UserDetails类型数据返回，SpringSecurity框架会比对密码是否正确，我们不用比对密码
        String password = xcUser.getPassword();

        //用户权限,如果不加报Cannot pass a null GrantedAuthority collection
        String[] authorities = {"p1"};

        xcUser.setPassword(""); // 为了安全考虑，专JSON前吧密码做空
        String userString = JSON.toJSONString(xcUser);
        UserDetails userDetails = User.withUsername(userString)
                //用户密码
                .password(password)
                //用户权限,暂时先不写
                .authorities(authorities).build();
        return userDetails;
    }

}
