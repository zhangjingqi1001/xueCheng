package com.xuecheng.base.utils;

import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        // 二维码最终其实就是一个网址
        System.out.println(qrCodeUtil.createQRCode("http://www.itcast.cn/", 200, 200));
    }

}
