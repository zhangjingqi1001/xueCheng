package com.xuecheng.auth.controller;

import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

/**
 * 微信登录地址
 */
@Slf4j
@Controller
public class WxLoginController {

    @Autowired
    WxAuthService wxAuthService;

    /**
     * 微信回调此接口，向服务传送一个授权码
     * <p>
     * 在此接口内我们要：远程调用微信令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库
     *
     * @param code  授权码
     * @param state 用于保持请求和回调的状态，授权请求后原样带回给第三方。该参数可用于防止csrf 攻击(跨站请求伪造攻击)，建议第三方带上该参数，可设置为简单的随机数加 session 进行校验
     * @return
     * @throws IOException
     */
    @RequestMapping("/wxLogin")
    public String wxLogin(String code, String state) throws IOException {
        log.debug("微信扫码回调,code:{},state:{}", code, state);
        //请求微信申请令牌，拿到令牌查询用户信息，将用户信息写入本项目数据库
        XcUser xcUser = wxAuthService.wxAuth(code);

        //暂时硬编写，目的是调试环境
        xcUser.setUsername("t1");
        if (xcUser == null) {
            return "redirect:http://www.51xuecheng.cn/error.html";
        }
        String username = xcUser.getUsername();

        // 重定向到登录界面自动登录  （注意！authType的值是wx）
        return "redirect:http://www.51xuecheng.cn/sign.html?username=" + username + "&authType=wx";
    }
}
