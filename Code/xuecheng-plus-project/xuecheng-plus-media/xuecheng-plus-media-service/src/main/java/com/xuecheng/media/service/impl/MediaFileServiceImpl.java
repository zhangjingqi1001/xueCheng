package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    private MinioClient minioClient;

    //除了视频文件以外的桶
    @Value("${minio.bucket.files}")
    private String bucket_medialFiles;

    //视频文件桶
    @Value("${minio.bucket.videofiles}")
    private String bucket_video;


    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    /**
     * 上传文件
     *
     * @param companyId           机构id
     * @param uploadFileParamsDto 上传文件信息
     * @param localFilePath       文件磁盘路径
     * @return 文件信息
     */
    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {
        //TODO 1.将文件上传到Minio

        //TODO 1.1 获取文件扩展名
        String filename = uploadFileParamsDto.getFilename();
        String extension = filename.substring(filename.lastIndexOf("."));

        //TODO 1.2 根据文件扩展名获取mimeType
        String mimeType = this.getMimeType(extension);

        //TODO 1.3 bucket，从nacos中读取
        //TODO 1.4 ObjectName约定在MinIo系统中存储的目录是年/月/日/图片文件
        //得到文件的路径defaultFolderPath
        String defaultFolderPath = this.getDefaultFolderPath();
        //最终存储的文件名是MD5值
        String fileMd5 = this.getFileMd5(new File(localFilePath));
        String ObjectName = defaultFolderPath+fileMd5+extension;
        boolean b = this.addMediaFilesToMinIO(localFilePath, bucket_medialFiles, ObjectName, mimeType);

        //TODO 2.将文件信息保存到数据库

        return null;
    }

    //

    /**
     * 将文件上传到MinIo
     *
     * @param bucket        桶
     * @param localFilePath 文件在本地的路径
     * @param objectName    上传到MinIo系统中时的文件名称
     * @param mimeType      上传的文件类型
     */
    public boolean addMediaFilesToMinIO(String localFilePath, String bucket, String objectName, String mimeType) {
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                    //桶，也就是目录
                    .bucket(bucket)
                    //指定本地文件的路径
                    .filename(localFilePath)
                    //上传到minio中的对象名，上传的文件存储到哪个对象中
                    .object("SpringBootApplicationTestsMinIo")
                    .contentType(mimeType)
                    //构建
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("上传文件出错，bucket:{},objectName:{},错误信息:{}", bucket, objectName, e.getMessage());
            return false;
        }
    }


    /**
     * 根据扩展名获取mimeType
     *
     * @param extension 扩展名
     */
    private String getMimeType(String extension) {
        if (extension == null) {
            //目的是防止空指针异常
            extension = "";
        }
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用mimeType，字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }

    /**
     * @return 获取文件默认存储目录路径 年/月/日
     */
    private String getDefaultFolderPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date()).replace("-", "/") + "/";
    }


    /**
     * 获取文件的md5
     *
     * @param file 文件
     * @return MD5值
     */
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fileInputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
