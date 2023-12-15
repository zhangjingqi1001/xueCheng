package com.xuecheng.base.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class XueChengPlusException extends RuntimeException {

   private static final long serialVersionUID = -378480393877627738L;

   private String errMessage;

    public XueChengPlusException() {
        super();
    }

    public XueChengPlusException(String errMessage) {
        super(errMessage);
        this.errMessage = errMessage;
    }


    public static void cast(CommonError commonError) {
        throw new XueChengPlusException(commonError.getErrMessage());
    }

    public static void cast(String errMessage) {
        throw new XueChengPlusException(errMessage);
    }

}
