package com.xuecheng.media.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理接口
 * @date 2022/9/6 11:29
 */
@Api(value = "媒资文件管理接口", tags = "媒资文件管理接口")
@RestController("/media")
public class MediaFilesController {


    @Autowired
    MediaFileService mediaFileService;


    @ApiOperation("媒资列表查询接口")
    @PostMapping("/files")
    public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto) {
        Long companyId = 1232141425L;
        return mediaFileService.queryMediaFiels(companyId, pageParams, queryMediaParamsDto);

    }

    /**
     * 对于请求内容：Content-Type: multipart/form-data;
     * 前端向后端传输一个文件，那后端程序就属于一个消费者,我们指定一下类型
     * form-data; name="filedata"; filename="具体的文件名称"
     * <p>
     * 我们可以使用@RequestPart指定一下前端向后端传输文件的名称
     * 用MultipartFile类型接收前端向后端传输的文件
     */
    @ApiOperation("上传图片")
    @PostMapping(value = "/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadFileResultDto upload(@RequestPart("filedata") MultipartFile filedata,
                                      @RequestParam(value = "objectName", required = false) String objectName) throws IOException {
        //此时已经接收到文件了，目前作为临时文件存储在内存中
        //1.创建一个临时文件,前缀是"minio"，后缀是“.temp”
        File tempFile = File.createTempFile("minio", ".temp");
        //2.将上传后的文件传输到临时文件中
        filedata.transferTo(tempFile);
        //3.取出临时文件的绝对路径
        String localFilePath = tempFile.getAbsolutePath();

        Long companyId = 1232141425L; //先写死，写认证授权系统时再进行
        //4.准备上传文件的信息
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        //filedata.getOriginalFilename()获取原始文件名称
        uploadFileParamsDto.setFilename(filedata.getOriginalFilename());
        //文件大小
        uploadFileParamsDto.setFileSize(filedata.getSize());
        //文件类型 001001在数据字典中代表图片
        uploadFileParamsDto.setFileType("001001");
        //调用service上传图片
        return mediaFileService.uploadFile(companyId, uploadFileParamsDto, localFilePath,objectName);
    }
}
