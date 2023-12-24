package com.xuecheng.media.api;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.service.MediaFileService;
import com.xuecheng.media.service.impl.MediaFileServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;

/**
 * 上传视频接口
 */
@Api(value = "大文件上传接口", tags = "大文件上传文件")
@Slf4j
@RestController
public class BigFilesController {

    @Autowired
    private MediaFileService mediaFileService;

    /**
     * 1.检查文件数据库有没有
     * 2.如果数据库有了再检查minio系统当用有没有（可能存在脏数据，数据库中有但是minio没有那也要传输）
     */
    @ApiOperation(value = "文件上传前检查文件")
    @PostMapping("/upload/checkfile")
    public RestResponse<Boolean> checkfile(@RequestParam("fileMd5") String fileMd5) {
        return mediaFileService.checkFile(fileMd5);
    }

    /**
     * 分块在数据库中是不存储的，但是可以向minio中查询分块是否存在
     * minio中有了就不再传了，若没有的话再传
     */
    @ApiOperation(value = "分块文件上传前的检测")
    @PostMapping("/upload/checkchunk")
    public RestResponse<Boolean> checkChunk(@RequestParam("fileMd5") String fileMd5,
                                            @RequestParam("chunk") int chunk) throws Exception {
        return mediaFileService.checkChunk(fileMd5, chunk);
    }

    @ApiOperation(value = "上传分块文件")
    @PostMapping("/upload/uploadchunk")
    public RestResponse uploadChunk(@RequestParam("file") MultipartFile file,
                                    @RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("chunk") int chunk) throws Exception {
        //1.创建一个临时文件,前缀是"minio"，后缀是“.temp”
        File tempFile = File.createTempFile("minio", ".temp");
        file.transferTo(tempFile);
        return mediaFileService.uploadChunk(fileMd5, chunk, tempFile.getAbsolutePath());
    }

    /**
     * @param fileMd5    文件md5值
     * @param fileName   合并分块之后要入库，fileName原始文件名要写在数据库
     * @param chunkTotal 总共分块数
     */
    @ApiOperation(value = "合并文件")
    @PostMapping("/upload/mergechunks")
    public RestResponse mergeChunks(@RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("fileName") String fileName,
                                    @RequestParam("chunkTotal") int chunkTotal) throws Exception {
        Long companyId = 1232141425L;
        //文件信息对象
        UploadFileParamsDto uploadFileParamsDto = new UploadFileParamsDto();
        uploadFileParamsDto.setFilename(fileName);
        uploadFileParamsDto.setTags("视图文件");
        uploadFileParamsDto.setFileType("001002");//数据字典代码 - 001002代表视频
        return mediaFileService.mergeChunks(companyId, fileMd5, chunkTotal, uploadFileParamsDto);
    }


}
