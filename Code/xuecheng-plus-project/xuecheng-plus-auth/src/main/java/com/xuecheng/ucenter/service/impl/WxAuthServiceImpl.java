package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


/**
 * @description 微信扫码认证
 */
@Slf4j
@Service("wx_authservice")
public class WxAuthServiceImpl implements AuthService, WxAuthService {

    @Autowired
    XcUserMapper xcUserMapper;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    XcUserRoleMapper xcUserRoleMapper;

    @Autowired
    WxAuthServiceImpl currentProxy;

    // 微信appid
    String appid = "wx17655f8047b85150";
    // 微信App秘钥
    String secret = "68918d65287802a19b1905cbda7eaa93";

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {
        //账号
        String username = authParamsDto.getUsername();
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (user == null) {
            //返回空表示用户不存在
            throw new RuntimeException("账号不存在");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(user, xcUserExt);
        return xcUserExt;
    }

    /**
     * 微信扫码认证：
     * 1.申请令牌
     * 2.携带令牌查询用户信息
     * 3.保存用户信息到数据库
     *
     * @param code 微信下发的授权码
     * @return
     */
    @Override
    public XcUser wxAuth(String code) {
        // 1.申请令牌
        Map<String, String> accessTokenMap = getAccess_token(code);
        //令牌
        String accessToken = accessTokenMap.get("access_token");
        //openid
        String openid = accessTokenMap.get("openid");

        // 2.携带令牌查询用户信息
        Map<String, String> userInfo = this.getUserInfo(accessToken, openid);

        // 3.保存用户信息到数据库
        // 避免事物失效（一个非事物方法调用事物方法）
        XcUser xcUser = currentProxy.addWxUser(userInfo);

        return xcUser;
    }

    /**
     * 申请令牌
     * 通过code获取access_token
     * https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
     *
     * @param code 微信下发的授权码
     * @return 根据文档发现，返回值是一个JSON信息
     * {
     * "access_token":"ACCESS_TOKEN",
     * "expires_in":7200,
     * "refresh_token":"REFRESH_TOKEN",
     * "openid":"OPENID",
     * "scope":"SCOPE",
     * "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     * }
     */
    private Map<String, String> getAccess_token(String code) {
        String url_template = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        String url = String.format(url_template, appid, secret, code);
        // 参数三：请求参数设置为null，因为我们拼接在url后面了 参数四：返回值类型，执行String类型就行
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, null, String.class);

        //获取响应结果
        String result = exchange.getBody();
        Map<String, String> map = JSON.parseObject(result, Map.class);
        return map;
    }

    /**
     * 携带令牌查询用户信息
     * http请求方式: GET
     * https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
     *
     * @param access_token 令牌
     * @param openid       返回值实例：
     *                     {
     *                     "openid":"OPENID",
     *                     "nickname":"NICKNAME",
     *                     "sex":1,
     *                     "province":"PROVINCE",
     *                     "city":"CITY",
     *                     "country":"COUNTRY",
     *                     "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
     *                     "privilege":[
     *                     "PRIVILEGE1",
     *                     "PRIVILEGE2"
     *                     ],
     *                     "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"
     *                     }
     */
    private Map<String, String> getUserInfo(String access_token, String openid) {
        String wxUrl_template = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        //请求微信地址
        String wxUrl = String.format(wxUrl_template, access_token, openid);

        log.info("调用微信接口申请access_token, url:{}", wxUrl);

        ResponseEntity<String> exchange = restTemplate.exchange(wxUrl, HttpMethod.GET, null, String.class);

        //防止乱码进行转码
        // 因为返回值格式是8859-1，我们转换成UTF-8，解决乱码问题
        String result = new String(exchange.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        Map<String, String> map = JSON.parseObject(result, Map.class);
        return map;
    }

    @Transactional
    public XcUser addWxUser(Map userInfo_map) {
        //wx_unionid是在微信开放平台上唯一的id
        //张三扫描微信二维码登录后会有一个unionid，李四扫描微信二维码登录后会有一个unionid
        String unionid = userInfo_map.get("unionid").toString();
        //根据unionid查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if (xcUser != null) {
            //说明查询到对应的用户了
            return xcUser;
        }
        // 没有查询到用户，新增用户
        String userId = UUID.randomUUID().toString();
        xcUser = new XcUser();
        xcUser.setId(userId);
        xcUser.setWxUnionid(unionid);
        //记录从微信得到的昵称
        xcUser.setNickname(userInfo_map.get("nickname").toString());
        xcUser.setUserpic(userInfo_map.get("headimgurl").toString());
        xcUser.setName(userInfo_map.get("nickname").toString());
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setUtype("101001");//学生类型
        xcUser.setStatus("1");//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);

        // 设置默认角色为 学生 角色
        XcUserRole xcUserRole = new XcUserRole();
        // 主键
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);
        xcUserRole.setRoleId("17");//学生角色
        // 操作的xc_user_role学生角色关系表
        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;
    }

}