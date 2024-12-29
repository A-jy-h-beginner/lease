package com.atguigu.lease.web.admin.vo.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Schema(description = "图像验证码")
@AllArgsConstructor
public class CaptchaVo {

    @Schema(description="验证码图片信息") // base64能把二进制数据转为String类型
    private String image;

    @Schema(description="验证码key")
    private String key;
}
