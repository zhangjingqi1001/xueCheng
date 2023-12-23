package com.xuecheng.base.model;

import lombok.Data;
import lombok.ToString;

/**
 * 通用结果类
 */
@Data
@ToString
public class RestResponse<T> {
    /**
     * 响应编码：0正常，-1错误
     */
    private int code;

    /**
     * 响应提示信息
     */
    private String msg;

    private T result;
    public RestResponse(){
        this(0,"success");
    }

    public RestResponse(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public RestResponse(int code, String msg, T result) {
        this.code = code;
        this.msg = msg;
        this.result = result;
    }
}
