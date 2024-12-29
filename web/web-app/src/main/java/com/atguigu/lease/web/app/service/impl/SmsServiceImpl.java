package com.atguigu.lease.web.app.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.atguigu.lease.web.app.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SmsServiceImpl implements SmsService {

    @Autowired
    private Client client;
    @Override
    public void sendCode(String phone, String code) {
        /* 发送验证码
        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumbers(phone); // 号码
        request.setSignName("阿里云短信服务"); // 签名
        request.setTemplateCode("SMS_111");
        request.setTemplateParam("{\"code\":\"" + code + "\"}");  // 模板
        // 发送请求
        try {
            client.sendSms(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }*/
        System.out.println("发送成功验证码：" + code);
    }
}
