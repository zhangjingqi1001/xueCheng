package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一异常处理类
 */
@Slf4j
@ControllerAdvice
@ResponseBody//返回JSON数据
//@RestControllerAdvice = @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    /**
     * 针对自定义异常进行处理的
     */
    @ResponseBody
    @ExceptionHandler(XueChengPlusException.class)//主桌这个异常
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)//状态码500
    public RestErrorResponse customException(XueChengPlusException e) {
//      捕捉到异常的信息后解析异常的信息
        log.error("【系统异常】{}", e.getErrMessage(), e);
        return new RestErrorResponse(e.getErrMessage());
    }

    /**
     * 捕捉除了自定义异常以外的其他异常
     */
    @ResponseBody
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)//状态码500
    public RestErrorResponse exception(Exception e) {
        log.error("【系统异常】{}", e.getMessage(), e);
        return new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());//执行过程异常，请重试

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e) {
//      这个异常对象有一个方法getBindingResult
        BindingResult bindingResult = e.getBindingResult();
        List<String> msgList = new ArrayList<>();
        //将错误信息放在msgList
        bindingResult.getFieldErrors().stream().forEach(item -> msgList.add(item.getDefaultMessage()));
        //拼接错误信息
        String msg = StringUtils.join(msgList, ",");
        log.error("【系统异常】{}", msg);
        return new RestErrorResponse(msg);
    }


}
