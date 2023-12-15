package com.xuecheng.base.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 和前端约定返回的异常信息模型
 * 错误响应参数包装
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestErrorResponse implements Serializable {

    private static final long serialVersionUID = 9026504397012666687L;

    private String errMessage;

}
