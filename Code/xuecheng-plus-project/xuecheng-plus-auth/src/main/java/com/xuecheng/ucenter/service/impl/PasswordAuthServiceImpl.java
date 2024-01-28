package com.xuecheng.ucenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.feignclient.CheckCodeClient;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 处理用户名密码登录验证
 */
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CheckCodeClient checkCodeClient;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        // 1.账号
        String username = authParamsDto.getUsername();

        // 2.校验验证码（校验认证码的服务在checkcode模块，利用feign向checkcode发送请求）

        String checkcode = authParamsDto.getCheckcode();
        String checkcodekey = authParamsDto.getCheckcodekey();
        if (StringUtils.isEmpty(checkcode) || StringUtils.isEmpty(checkcodekey)){
            throw new RuntimeException("请输入验证码");
        }
        Boolean verify = checkCodeClient.verify(checkcodekey, checkcode);
        if (verify == null || !verify){
            throw new RuntimeException("验证码输入错误");
        }
        // 3.校验账号是否存在
        // 3.1 根据username账号查询数据库
        // 因为账号是不可能重复的，直接selectOne即可
        LambdaQueryWrapper<XcUser> lqw = new LambdaQueryWrapper<>();
        lqw.eq(XcUser::getUsername, username);
        XcUser xcUser = xcUserMapper.selectOne(lqw);
        // 3.2 查询不到用户，返回null即可
        if (xcUser == null) {
            throw new RuntimeException("账号不存在");
        }

        // 4.验证密码是否正确
        // 用户真正的密码
        String password = xcUser.getPassword();
        // 用户输入的密码
        String passwordForm = authParamsDto.getPassword();
        // 校验密码
        boolean matches = passwordEncoder.matches(passwordForm, password);
        if (!matches) {
            throw new RuntimeException("用户账号或密码错误");
        }

        // 5.这个地方到授权后会再改，先这么写着
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);
        return xcUserExt;
    }
}
