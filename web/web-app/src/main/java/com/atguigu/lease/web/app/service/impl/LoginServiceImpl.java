package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.common.constant.RedisConstant;
import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.common.utils.CodeUtil;
import com.atguigu.lease.common.utils.JwtUtil;
import com.atguigu.lease.model.entity.UserInfo;
import com.atguigu.lease.model.enums.BaseStatus;
import com.atguigu.lease.web.app.service.LoginService;
import com.atguigu.lease.web.app.service.SmsService;
import com.atguigu.lease.web.app.service.UserInfoService;
import com.atguigu.lease.web.app.vo.user.LoginVo;
import com.atguigu.lease.web.app.vo.user.UserInfoVo;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SmsService smsService;
    @Autowired
    private UserInfoService userInfoService;
    @Override
    public void getCode(String phone) {
        // 生成验证码
        // k-phone v-code redis
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        String s = operations.get(RedisConstant.APP_LOGIN_PREFIX + phone);
        // 限制发送频率
        if(s != null){
            throw new LeaseException(ResultCodeEnum.APP_SEND_SMS_TOO_OFTEN);
        }
        String code = CodeUtil.getRandomCode(6);
        smsService.sendCode(phone, code);
        // 存到redis
        operations.set(RedisConstant.APP_LOGIN_PREFIX + phone, code, RedisConstant.ADMIN_LOGIN_CAPTCHA_TTL_SEC, TimeUnit.SECONDS);
    }

    @Override
    public String login(LoginVo loginVo) {
        // 判断手机号验证码是否为空
        if(loginVo.getPhone() == null){
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_PHONE_EMPTY);
        }
        if(loginVo.getCode() == null){
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_CODE_EMPTY);
        }
        // redis查询验证码
        String key = RedisConstant.APP_LOGIN_PREFIX + loginVo.getPhone();
        String code = stringRedisTemplate.opsForValue().get(key);
        if(code == null){
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_CODE_EXPIRED);
        }
        if(!loginVo.getCode().equals(code)){
            throw new LeaseException(ResultCodeEnum.APP_LOGIN_CODE_ERROR);
        }
        // 查询用户信息
        UserInfo userInfo = userInfoService.lambdaQuery()
                .eq(UserInfo::getPhone, loginVo.getPhone())
                .one();
        // 新用户
        if(userInfo == null){
            userInfo = new UserInfo();
            userInfo.setPhone(loginVo.getPhone());
            userInfo.setStatus(BaseStatus.ENABLE);
            userInfo.setNickname("用户" + loginVo.getPhone().substring(7));
            userInfoService.save(userInfo); // 数据会回显到userInfo的id
        }else{
            if(userInfo.getStatus().equals(BaseStatus.DISABLE)){ // 禁用
                throw new LeaseException(ResultCodeEnum.APP_ACCOUNT_DISABLED_ERROR);
            }
        }
        return JwtUtil.getToken(userInfo.getId(), userInfo.getPhone());
    }

    @Override
    public UserInfoVo getLoginUserById(Long userId) {
        UserInfo userInfo = userInfoService.getById(userId);
        return new UserInfoVo(userInfo.getNickname(), userInfo.getAvatarUrl());
    }
}
