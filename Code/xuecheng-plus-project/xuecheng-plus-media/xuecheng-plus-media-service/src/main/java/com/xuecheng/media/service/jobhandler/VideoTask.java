package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VideoTask {

    @Autowired
    MediaFileProcessService mediaFileProcessService;

    @Autowired
    MediaFileService mediaFileService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegPath;

    /**
     * 视频处理任务
     * 分片广播任务
     */
    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();//执行器的序号，从0开始
        int shardTotal = XxlJobHelper.getShardTotal();//执行器总数

        //只要有了上面两个参数，我们就可以人为确定我们执行器执行哪一部分
        System.out.println("shardIndex:" + shardIndex);
        System.out.println("shardTotal:" + shardTotal);

        //确定cpu的核心数
        int processors = Runtime.getRuntime().availableProcessors();

        //TODO 1.查询待处理任务
        //参数3：查询多少个任务
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
        //任务数量
        int size = mediaProcessList.size();
        log.debug("取到的视频处理任务数：" + size);
        if (size <= 0) {
            return;
        }
        //使用一个计数器
        //默认值就是size
        //这个地方为什么会有一个计数器？
        //因为下面的代码forEach遍历完其实videoJobHandler方法已经完成了，至于executorService.execute里的代码是在新线程中执行的
        //但是线程不执行完我们不允许videoJobHandler结束，如果结束的话调度中心就会给videoJobHandler执行器派送任务，很有可能任务重复执行
        //所以当计数器减到0后，让videoJobHandler方法才结束
        CountDownLatch countDownLatch = new CountDownLatch(size);
        //TODO 1.1创建一个线程池,有多少个任务我们就创建多少个线程
        //当所有的任务处理完成后，线程池自己就销毁了
        ExecutorService executorService = Executors.newFixedThreadPool(size);

        mediaProcessList.forEach(mediaProcess -> {
            //TODO 1.2 execute将任务加入线程池
            executorService.execute(() -> {
                try {
                    //TODO 任务的执行逻辑
                    //TODO 2.争抢任务(开启任务)
                    //startTask方法其实就是更新一下media_process数据库表中status字段为4，表示正在处理，此时其他的线程就不会再拿到这个任务
                    boolean b = mediaFileProcessService.startTask(mediaProcess.getId());
                    if (!b) {
                        log.debug("抢占任务失败，任务id:{}", mediaProcess.getId());
                        return;
                    }
                    //TODO 3.执行视频转码
                    //ffmpeg.exe的路径
                    //String ffmpegPath = "D:\\zhangjingqi\\soft\\ffmpeg-4.3.1\\bin\\ffmpeg.exe";//ffmpeg的安装位置
                    //TODO 3.1 下载minio视频到本地
                    //下载到本地的一个临时文件file
                    File file = mediaFileService.downloadFileFromMinIO(mediaProcess.getBucket(), mediaProcess.getFilePath());
                    if (file == null) {
                        log.debug("下载视频出错，任务id:{},Bucket:{},objectName:{}", mediaProcess.getId(), mediaProcess.getBucket(), mediaProcess.getFilePath());
                        //保存任务失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(), null, "下载minio视频到本地失败");
                        return;
                    }
                    //源avi视频的路径
                    //String video_path = "D:\\develop\\bigfile_test\\nacos01.avi";
                    String video_path = file.getAbsolutePath();
                    //文件id其实是MD5值
                    String fileId = mediaProcess.getFileId();
                    //转换后mp4文件的名称
                    //String mp4_name = "nacos01.mp4";
                    String mp4_name = fileId + ".mp4";
                    //转换后mp4文件的路径
                    //先创建一个临时文件，作为转换后的文件
                    File mp4File;
                    try {
                        mp4File = File.createTempFile("minio" + fileId, ".mp4");
                    } catch (IOException e) {
                        log.debug("创建临时文件异常：{}", e.getMessage());
                        //保存任务失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(), null, "创建本地临时文件失败");
                        return;
                    }
                    //String mp4_path = "D:\\develop\\bigfile_test\\";
                    String mp4_path = mp4File.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegPath, video_path, mp4_name, mp4_path);
                    //TODO 3.2 开始视频转换，成功将返回success，失败的话会返回失败的原因
                    String result = videoUtil.generateMp4();
                    if (!"success".equals(result)) {
                        log.debug("视频转码失败，原因：{}，bucket:{},ObjectName:{}", result, mediaProcess.getBucket(), mediaProcess.getFilePath());
                        //保存任务失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(), null, result);
                        return;
                    }
                    //TODO 4.上传到minio
                    boolean addMediaFilesToMinIO = mediaFileService.addMediaFilesToMinIO(mp4_path, mediaProcess.getBucket(), mediaProcess.getFilePath(), "video/mp4");
                    if (!addMediaFilesToMinIO) {
                        log.debug("上传转码视频失败，taskid:{},bucket:{},ObjectName:{}", mediaProcess.getId(), mediaProcess.getBucket(), mediaProcess.getFilePath());
                        //保存任务失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", mediaProcess.getFileId(), null, "上传转码后视频失败");
                        return;
                    }
                    //TODO 5.保存任务的处理结果
                    //拼接mp4文件的url
                    String filePath = this.getFilePathByMD5(mediaProcess.getFileId(), ".mp4");
                    //保存任务的状态为成功
                    mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "2", mediaProcess.getFileId(), filePath, "");

                } finally {
                    //finally的目的是无论如何都要让计数器减一
                    //TODO 6. 计数器减一
                    countDownLatch.countDown();
                }

            });
        });
        //堵塞
        //什么时候放行？当计数器为0时，下面的堵塞会放行
        //并且最多堵塞30分钟，超过30分钟就不等了，类似一个保底的策略
        countDownLatch.await(30,TimeUnit.MINUTES);

    }

    /**
     * @param fileMd5 文件md5值
     * @param fileExt 文件扩展名
     */
    private String getFilePathByMD5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

}
