package com.xuecheng.content.feignclient;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

public class MediaServiceClientFallback implements MediaServiceClient{
    
    // 重写upload方法
    @Override
    public String upload(@RequestPart("filedata") MultipartFile filedata,
                                      @RequestParam(value = "objectName", required = false) String objectName){
        
        return null;
    }
    
}