package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    MediaProcessMapper mediaProcessMapper;

    /**
     * 代理对象
     */
    @Autowired
    MediaFileService currentProxy;

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
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath, String objectName) {
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
        //存储到minio中的对象名(带目录)
        if(StringUtils.isEmpty(objectName)){
            objectName =  defaultFolderPath + fileMd5 + extension;
        }
         objectName = defaultFolderPath + fileMd5 + extension;
        //TODO 1.5 上传文件到Minio
        boolean result = this.addMediaFilesToMinIO(localFilePath, bucket_medialFiles, objectName, mimeType);

        if (!result) {
            XueChengPlusException.cast("上传文件失败");
        }
        //TODO 2.将文件信息保存到数据库
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_medialFiles, objectName);
        if (mediaFiles == null) {
            XueChengPlusException.cast("文件上传后保存信息失败");
        }
        //再更新一下course_base表中pic字段
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
        return uploadFileResultDto;
    }

    /**
     * @param companyId           机构id
     * @param fileMd5             文件md5值
     * @param uploadFileParamsDto 上传文件的信息
     * @param bucket              桶
     * @param objectName          对象名称
     * @return com.xuecheng.media.model.po.MediaFiles
     * @description 将文件信息添加到文件表
     */

    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
        //根据文件MD5值向数据库查找文件信息
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(fileMd5);//文件信息的主键是文件的MD5值
            mediaFiles.setCompanyId(companyId);//机构ID
            mediaFiles.setBucket(bucket);//桶
            mediaFiles.setFilePath(objectName);//对象名
            mediaFiles.setFileId(fileMd5);//file_id字段
            mediaFiles.setUrl("/" + bucket + "/" + objectName);//url
            mediaFiles.setCreateDate(LocalDateTime.now());//上传时间
            mediaFiles.setStatus("1");//状态 1正常 0不展示
            mediaFiles.setAuditStatus("002003");//审核状态 002003审核通过
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert <= 0) {
                log.debug("向数据库保存文件失败bucket:{},objectName:{}", bucket, objectName);
                return null;
            }
        }
        //TODO 记录待处理任务
        this.addWaitingTask(mediaFiles);
        return mediaFiles;
    }

    /**
     * 添加待处理任务
     *
     * @param mediaFiles 媒资文件信息，是media_files表的实体类
     */
    private void addWaitingTask(MediaFiles mediaFiles) {
        //必须要判断一下，通过mimeType判断文件类型一定是avi视频文件才会添加到待处理任务中，其他的问题并不会
        //文件名称
        String filename = mediaFiles.getFilename();
        //通过文件的扩展名获取mimeType
        String mimeType = getMimeType(filename.substring(filename.lastIndexOf(".")));
        if (mimeType.equals("video/x-msvideo")) {
            //说明文件是avi文件，需要写入待处理任务表
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles, mediaProcess);
            mediaProcess.setStatus("1");//未处理
            mediaProcess.setFailCount(0);//失败次数默认为0
            mediaProcess.setUrl(null);
            //向mediaProcess插入记录
            mediaProcessMapper.insert(mediaProcess);
        }
    }

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
                    .object(objectName).contentType(mimeType)
                    //构建
                    .build();
            minioClient.uploadObject(uploadObjectArgs);
            log.debug("上传文件到minio成功，bucket:{},objectName:{}", bucket, objectName);
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


    /**
     * 1.首先查询数据库，如果文件不在数据库中表明文件不在
     * 2.如果文件在数据库中再查询minio系统，
     *
     * @param fileMd5 文件的md5
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查文件是否存在
     */
    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        //TODO 1.查询数据库
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        //TODO 2.如果数据库存在再查询minio
        if (mediaFiles != null) {
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    //mediaFiles会有记录
                    .bucket(mediaFiles.getBucket())
                    .object(mediaFiles.getFilePath())
                    .build();
            try {
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                if (inputStream != null) {
                    //文件已经存在
                    return RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //文件不存在
        return RestResponse.success(false);
    }

    /**
     * 分块不会存在于数据库，直接查询minio系统即可
     *
     * @param fileMd5    文件的md5
     * @param chunkIndex 分块序号
     * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
     * @description 检查分块是否存在
     */
    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        //TODO 1.判断fileMd5对应的文件已经合并成一个完整的文件了，如果有了的话，那也不需要检查分块了
        //判断Minio系统中是否有已经有合并的文件了，如果有的话没有分块所在路径也无所谓
        RestResponse<Boolean> booleanRestResponse = this.checkFile(fileMd5);
        if (booleanRestResponse.getResult()) {
            //文件已经存在
            return RestResponse.success(true);
        }

        //运行到这里说明完整的文件不存在，需要商场对应的分块文件
        //TODO 2.根据MD5得到分块文件的目录路径
        //分块存储路径：md5前两位为两个目录，MD5值也是一层目录，chunk目录存储分块文件
        //这个目录不存在也不一定代表着我们要上传分块，因为上传完分块之后我们会调用Minio的SDK将所有的分块删除然后生成一个完整的文件的
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);

        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                //视频的桶
                .bucket(bucket_video)
                //文件名是目录路径+分块序号
                .object(chunkFileFolderPath + chunkIndex).build();
        try {
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            if (inputStream != null) {
                //文件已经存在
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResponse.success(false);
    }


    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/chunk/";
    }


    /**
     * @param fileMd5            文件md5
     * @param chunk              分块序号
     * @param localChunkFilePath 本地文件路径
     * @return com.xuecheng.base.model.RestResponse
     * @description 上传分块
     */
    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
        //TODO 将分块文件上传到minio
        //传空默认返回类型MediaType.APPLICATION_OCTET_STREAM_VALUE application/octet-stream未知流类型
        String mimeType = getMimeType(null);
        //获取分块文件的目录
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        boolean b = this.addMediaFilesToMinIO(localChunkFilePath, bucket_video, chunkFileFolderPath + chunk, mimeType);
        if (!b) {
            //false
            return RestResponse.validfail(false, "上传分块文件{" + fileMd5 + "/" + chunk + "}失败");
        }
        //上传分块文件成功
        return RestResponse.success(true);
    }

    /**
     * 为什么又companyId 机构ID？
     * 分布式文件系统空间不是随便使用的，比如某个机构传输的课程很多很多，那我们就可以收费了（比如超过1Tb便开始收费）
     * 知道了companyId我们就知道是谁传的，也知道这些机构用了多少GB
     *
     * @param companyId           机构id
     * @param fileMd5             文件md5
     * @param chunkTotal          分块总和
     * @param uploadFileParamsDto 文件信息（要入库）
     * @return com.xuecheng.base.model.RestResponse
     * @description 合并分块
     */
    @Override
    public RestResponse mergeChunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //TODO 0.如果已经有了合并分块后对应的文件的话，就不用再合并了
        //判断Minio系统中是否有已经有合并的文件了，如果有的话没有分块所在路径也无所谓
        RestResponse<Boolean> booleanRestResponse = this.checkFile(fileMd5);
        if (booleanRestResponse.getResult()) {
            //getResult()返回值是true的话，表示文件已经存在，不需要合并了
            return RestResponse.success(true);
        }

        //TODO 1.获取所有分块文件
        List<ComposeSource> sources = new ArrayList<>();
        //1.1 分块文件路径
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        for (int i = 0; i < chunkTotal; i++) {
            sources.add(ComposeSource.builder()
                    //分块文件所在桶
                    .bucket(bucket_video)
                    //分块文件名称
                    .object(chunkFileFolderPath + i).build());
        }
        //1.2 指定合并后文件存储在哪里
        String filename = uploadFileParamsDto.getFilename();
        String fileExt = filename.substring(filename.lastIndexOf("."));
        //1.3 获取对象存储名
        String filePathByMD5 = this.getFilePathByMD5(fileMd5, fileExt);
        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                //指定合并后文件在哪个桶里
                .bucket(bucket_video)
                //最终合并后的文件路径及名称
                .object(filePathByMD5)
                //指定分块源文件
                .sources(sources)
                .build();
        //TODO 2.合并分块
        try {
            minioClient.composeObject(composeObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件出错：bucket:{},objectName:{},错误信息:{}", bucket_video, filePathByMD5, e.getMessage());
            return RestResponse.validfail(false, "合并文件出错");
        }

        //TODO 3.校验合并后的文件与原文件是否一致
        //3.1校验时先要把文件下载下来
        File tempFile = this.downloadFileFromMinIO(bucket_video, filePathByMD5);
        //3.2 比较原文件与临时文件的MD5值
        //将FileInputStream放在括号里，当try..catch执行结束后会自动关闭流，不用加finally了
        try (FileInputStream fis = new FileInputStream(tempFile)) {
            String mergeFile_md5 = DigestUtils.md5Hex(fis);
            if (!fileMd5.equals(mergeFile_md5)) {
                log.error("校验合并文件md5值不一致，原始文件{}，合并文件{}", fileMd5, mergeFile_md5);
                return RestResponse.validfail(false, "文件校验失败");
            }

            //保存一下文件信息 - 文件大小
            uploadFileParamsDto.setFileSize(tempFile.length());

        } catch (IOException e) {
            e.printStackTrace();
            return RestResponse.validfail(false, "文件校验失败");
        }

        //TODO 4.文件信息
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_video, filePathByMD5);
        if (mediaFiles == null) {
            return RestResponse.validfail(false, "文件入库失败");
        }

        //TODO 5.清理分块文件
        //5.1获取分块文件路径
        //this.getChunkFileFolderPath(fileMd5);
        this.clearChunkFiles(chunkFileFolderPath, chunkTotal);


        //TODO 6.向数据库中记录待处理任务
        return RestResponse.success(true);
    }

    /**
     * 从minio下载文件
     *
     * @param bucket     桶
     * @param objectName 对象名称
     * @return 下载后的临时文件
     */
    public File downloadFileFromMinIO(String bucket, String objectName) {
        //临时文件
        File minioFile = null;
        FileOutputStream outputStream = null;
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            //创建临时文件
            minioFile = File.createTempFile("minio", ".merge");
            outputStream = new FileOutputStream(minioFile);
            //将数据拷贝到outputStream流对应的minioFile临时文件中
            IOUtils.copy(stream, outputStream);
            return minioFile;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    /**
     * @param fileMd5 文件md5值
     * @param fileExt 文件扩展名
     */
    private String getFilePathByMD5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    /**
     * 清除分块文件
     *
     * @param chunkFileFolderPath 分块文件路径
     * @param chunkTotal          分块文件总数
     */
    private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal) {
        //需要参数removeObjectsArgs

        //Iterable<DeleteObject> objects =
        List<DeleteObject> objects = Stream.iterate(0, i -> ++i)
                .limit(chunkTotal)
                //String.concat函数用于拼接字符串
                .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                .collect(Collectors.toList());

        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder()
                //指定要清理的分块文件的桶
                .bucket(bucket_video)
                //需要一个Iterable<DeleteObject>迭代器
                .objects(objects)
                .build();
        //执行了这段方法并没有真正的删除，还需要遍历一下
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
        results.forEach(f -> {
            try {
                //get方法执行之后才是真正的删除了
                DeleteError deleteError = f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
//        或者是下面这种遍历方式，都是可以的
//        for (Result<DeleteError> deleteError:results){
//            DeleteError error = deleteError.get();
//        }

    }

    /**
     * 根据媒资id获取媒资信息
     */
    @Override
    public MediaFiles getFileById(String mediaId) {
        return mediaFilesMapper.selectById(mediaId);
    }
}
