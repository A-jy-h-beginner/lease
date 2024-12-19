package com.atguigu.lease.common.exception;

import com.atguigu.lease.common.result.Result;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {  // 全局异常处理
    @ExceptionHandler(Exception.class)
    public Result handlerException(Exception e){
        System.out.println("异常：" + e);
        return Result.fail();
    }

    @ExceptionHandler(LeaseException.class)
    public Result handlerException(LeaseException e){
        System.out.println("异常：" + e);
        return Result.fail(e.getCode(), e.getMessage());
    }
}
