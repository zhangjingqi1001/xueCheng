package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试minio的SDK
 */
@SpringBootTest
public class MinioTest {
    MinioClient minioClient =
            MinioClient.builder()
                    //这个地方是运行minio后展示的地址
                    .endpoint("http://192.168.101.65:9000")
                    //账号和密码
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void test_upload() {

        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流


        try {
            // 判断桶testbucket是否存在，如果不存在的话就进创建
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket("testbucket").build());
            if (!found) {
                // Make a new bucket called 'asiatrip'.
                minioClient.makeBucket(MakeBucketArgs.builder().bucket("testbucket").build());
            } else {
                System.out.println("Bucket 'asiatrip' already exists.");
            }


            ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
            mimeType = extensionMatch.getMimeType();

            //minioClient.uploadObject上传文件，需要一个UploadObjectArgs类型参数
            //上传文件的参数信息： UploadObjectArgs.builder()进行构建
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            //桶，也就是目录
                            .bucket("testbucket")
                            //指定本地文件的路径
                            .filename("E:\\SpringBootApplicationTests.mp4")
                            //上传到minio中的对象名，上传的文件存储到哪个对象中
                            //下面这个是直接在桶下存储文件
                            //.object("SpringBootApplicationTestsMinIo")
                            //下面这个是存储在桶/test/目录下
                            .object("test/SpringBootApplicationTestsMinIo")
                            //设置媒体文件类型,可以通过扩展名得到媒体资源类型
                            .contentType(mimeType)
                            //构建
                            .build());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void delete() {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            //桶
                            .bucket("testbucket")
                            //要删除的对象
                            .object("SpringBootApplicationTestsMinIo")
                            .build());
            System.out.println("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("删除失败");
        }
    }

    //查询文件
    @Test
    public void getFile() {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket("testbucket")
                .object("test/SpringBootApplicationTestsMinIo")
                .build();
        try {
            //查询远程服务器获取到衣蛾流对象
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            //输出流，下载到哪里
            FileOutputStream outputStream = new FileOutputStream(new File("E:\\TestsMinIo.java"));
            //import org.apache.commons.io.IOUtils;
            IOUtils.copy(inputStream, outputStream);

            //校验文件的完整性，对文件的内容进行md5
            //对minio上的文件进行MD5摘要算法，对下载下来的文件进行摘要算法，如果两者MD5一样，说明下载的文件是完整的
            //import org.apache.commons.codec.digest.DigestUtils;
            //这个参数不要写远程流minioClient.getObject(getObjectArgs)，会不稳定或者有问题
            //String source_md5 = DigestUtils.md5Hex(inputStream);
            FileInputStream fileInputStream = new FileInputStream(new File("E:\\SpringBootApplicationTests.java"));
            //我们这个地方是拿到最开始上传到minio的原文件的MD5与从下载下面的文件MD5进行对比
            String source_md5 = DigestUtils.md5Hex(fileInputStream);

            String local_md5 = DigestUtils.md5Hex(new FileInputStream(new File("E:\\TestsMinIo.java")));


            if (source_md5.equals(local_md5)) {
                System.out.println("下载成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将分块文件上传到minio
     */
    @Test
    public void uploadChunk() throws Exception {
        //获取所有的分块文件
        File file = new File("E:\\chunk\\");
        File[] files = file.listFiles();
        for (File chunkFile : files) {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            //桶，也就是目录
                            .bucket("testbucket")
                            //指定本地文件的路径
                            .filename(chunkFile.getAbsolutePath())
                            //对象名
                            .object("chunk/" + chunkFile.getName())
                            //构建
                            .build());
            System.out.println("上传分块" + chunkFile.getName() + "成功");
        }
    }

    /**
     * 调用minio接口合并分块
     */
    @Test
    public void merge() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        //分块文件集合
        List<ComposeSource> sources = new ArrayList<>();
        sources.add(ComposeSource.builder()
                //分块文件所在桶
                .bucket("testbucket")
                //分块文件名称
                .object("chunk/0")
                .build());
        sources.add(ComposeSource.builder()
                //分块文件所在桶
                .bucket("testbucket")
                //分块文件名称
                .object("chunk/1")
                .build());

        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                //指定合并后文件在哪个桶里
                .bucket("testbucket")
                //最终合并后的文件路径及名称
                .object("merge/merge01.mp4")
                //指定分块源文件
                .sources(sources)
                .build();
        //合并分块
        minioClient.composeObject(composeObjectArgs);
    }

    /**
     * 批量清理分块文件
     */
    @Test
    public void test2() {

    }


}
