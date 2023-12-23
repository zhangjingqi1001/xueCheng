package com.xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 测试大文件上传方法
 */

public class BigFileTest {
    /**
     * 测试分块
     */
    @Test
    public void testChunk() throws Exception {
        //TODO 1.获取源文件
        File sourceFile = new File("E:\\歌.mp4");

        //TODO 2.定义基本参数
        //2.1 分块文件存储路径
        String chunkFilePath = "E:\\chunk\\";
        //2.2 分块文件的大小 1024*1024*1 代表1M，5M的话乘5即可（也就是最小单位是字节byte 1024个byte是1k）
        int chunkSize = 1024 * 1024 * 1;
        //2.3 分块文件大小
        //Math.ceil表示向上取整
        //sourceFile.length()是获取文件的大小是多少byte字节
        int chunkNum = (int) Math.ceil((sourceFile.length() * 1.0) / chunkSize);

        //TODO 3.从源文件中读数据，向分块文件中写数据
        //RandomAccessFile流既可以读又可以写
        //参数一：File类型  参数二：是读（“r”）还是写"rw"
        RandomAccessFile raf_r = new RandomAccessFile(sourceFile, "r");
        //缓存区,1k
        byte[] bytes = new byte[1024];
        //TODO 3.1 创建分块文件
        for (int i = 0; i < chunkNum; i++) {
            File chunkFile = new File(chunkFilePath + i);
            //分块文件写入流
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            //将数据读取到缓冲区中raf_r.read(bytes)
            while ((len = raf_r.read(bytes)) != -1) {
                //向临时文件中进行写入
                raf_rw.write(bytes, 0, len);
                //如果分块文件chunkFile的大小大于等于我们规定的分块文件的chunkSize大小，就不要再继续了
                if (chunkFile.length() >= chunkSize) {
                    break;
                }
            }
            raf_rw.close();
        }
        raf_r.close();
    }

    /**
     * 测试合并
     */
    @Test
    public void testMerge() throws Exception {
        //TODO 1.基本参数
        //分块文件目录
        File chunkFolder = new File("E:\\chunk\\");
        //源文件
        File sourceFile = new File("E:\\歌.mp4");
        //合并后的文件在哪里
        File mergeFile = new File("E:\\chunk\\歌Copy.mp4");

        //TODO 2.取出所有分块文件,此时的顺序可能是无序的
        File[] files = chunkFolder.listFiles();
        //将数组转换成List
        List<File> fileList = Arrays.asList(files);
        //利用Comparator进行排序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                //升序
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });

        //TODO 3.合并分块文件
        //缓存区,1k
        byte[] bytes = new byte[1024];

        //向合并分块的流
        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
        for (File file : fileList) {
            //向读取分块文件
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int len = -1;
            while ((len = raf_r.read(bytes)) != -1) {
                raf_rw.write(bytes, 0, len);
            }
            raf_r.close();
        }
        raf_rw.close();

        //TODO 校验是否合并成功
        //合并文件完成后比对合并的文件MD5值域源文件MD5值
        FileInputStream fileInputStream = new FileInputStream(sourceFile);
        FileInputStream mergeFileStream = new FileInputStream(mergeFile);
        //取出原始文件的md5
        String originalMd5 = DigestUtils.md5Hex(fileInputStream);
        //取出合并文件的md5进行比较
        String mergeFileMd5 = DigestUtils.md5Hex(mergeFileStream);

        if (originalMd5.equals(mergeFileMd5)) {
            System.out.println("合并文件成功");
        } else {
            System.out.println("合并文件失败");
        }

    }
}
