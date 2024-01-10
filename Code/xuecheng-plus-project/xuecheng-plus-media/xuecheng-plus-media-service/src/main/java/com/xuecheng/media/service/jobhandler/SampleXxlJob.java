package com.xuecheng.media.service.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * XxlJob开发示例（Bean模式）
 *
 * 开发步骤：
 *      1、任务开发：在Spring Bean实例中，开发Job方法；
 *      2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 *      3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 *      4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */

/**
 * 任务类
 */
@Component
public class SampleXxlJob {
    private static Logger logger = LoggerFactory.getLogger(SampleXxlJob.class);


    /**
     * 执行器拿到任务后就会执行这个方法
     * 具体的任务方法
     */
    @XxlJob("demoJobHandler") //任务名称是demoJobHandler
    public void demoJobHandler() throws Exception {
        System.out.println("处理视频.....");
        //任务执行逻辑...
    }

    /**
     * 执行器拿到任务后就会执行这个方法
     * 具体的任务方法
     */
    @XxlJob("demoJobHandler2")
    public void demoJobHandler2() throws Exception {
        System.out.println("处理文档.....");
        //任务执行逻辑....
    }

    /**
     * 2、分片广播任务
     */
    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();//执行器的序号，从0开始
        int shardTotal = XxlJobHelper.getShardTotal();//执行器总数

        //只要有了上面两个参数，我们就可以人为确定我们执行器执行哪一部分
        System.out.println("shardIndex:"+shardIndex);
        System.out.println("shardTotal:"+shardTotal);
    }

//    @XxlJob("videoJobHandler")
//    public void videoJobHandler() throws Exception {
//
//        // 分片参数
//        int shardIndex = XxlJobHelper.getShardIndex();
//        int shardTotal = XxlJobHelper.getShardTotal();
//        List<MediaProcess> mediaProcessList = null;
//        int size = 0;
//        try {
//            //取出cpu核心数作为一次处理数据的条数
//            int processors = Runtime.getRuntime().availableProcessors();
//            //一次处理视频数量不要超过cpu核心数
//            mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);
//            size = mediaProcessList.size();
//            log.debug("取出待处理视频任务{}条", size);
//            if (size < 0) {
//                return;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return;
//        }
//        //启动size个线程的线程池
//        ExecutorService threadPool = Executors.newFixedThreadPool(size);
//        //计数器
//        CountDownLatch countDownLatch = new CountDownLatch(size);
//        //将处理任务加入线程池
//        mediaProcessList.forEach(mediaProcess -> {
//            threadPool.execute(() -> {
//                try {
//                    //任务id
//                    Long taskId = mediaProcess.getId();
//                    //抢占任务
//                    boolean b = mediaFileProcessService.startTask(taskId);
//                    if (!b) {
//                        return;
//                    }
//                    log.debug("开始执行任务:{}", mediaProcess);
//                    //下边是处理逻辑
//                    //桶
//                    String bucket = mediaProcess.getBucket();
//                    //存储路径
//                    String filePath = mediaProcess.getFilePath();
//                    //原始视频的md5值
//                    String fileId = mediaProcess.getFileId();
//                    //原始文件名称
//                    String filename = mediaProcess.getFilename();
//                    //将要处理的文件下载到服务器上
//                    File originalFile = mediaFileService.downloadFileFromMinIO(mediaProcess.getBucket(), mediaProcess.getFilePath());
//                    if (originalFile == null) {
//                        log.debug("下载待处理文件失败,originalFile:{}", mediaProcess.getBucket().concat(mediaProcess.getFilePath()));
//                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "下载待处理文件失败");
//                        return;
//                    }
//                    //处理结束的视频文件
//                    File mp4File = null;
//                    try {
//                        mp4File = File.createTempFile("mp4", ".mp4");
//                    } catch (IOException e) {
//                        log.error("创建mp4临时文件失败");
//                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "创建mp4临时文件失败");
//                        return;
//                    }
//                    //视频处理结果
//                    String result = "";
//                    try {
//                        //开始处理视频
//                        Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, originalFile.getAbsolutePath(), mp4File.getName(), mp4File.getAbsolutePath());
//                        //开始视频转换，成功将返回success
//                        result = videoUtil.generateMp4();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        log.error("处理视频文件:{},出错:{}", mediaProcess.getFilePath(), e.getMessage());
//                    }
//                    if (!result.equals("success")) {
//                        //记录错误信息
//                        log.error("处理视频失败,视频地址:{},错误信息:{}", bucket + filePath, result);
//                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, result);
//                        return;
//                    }
//
//                    //将mp4上传至minio
//                    //mp4在minio的存储路径
//                    String objectName = getFilePath(fileId, ".mp4");
//                    //访问url
//                    String url = "/" + bucket + "/" + objectName;
//                    try {
//                        mediaFileService.addMediaFilesToMinIO(mp4File.getAbsolutePath(), "video/mp4", bucket, objectName);
//                        //将url存储至数据，并更新状态为成功，并将待处理视频记录删除存入历史
//                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "2", fileId, url, null);
//                    } catch (Exception e) {
//                        log.error("上传视频失败或入库失败,视频地址:{},错误信息:{}", bucket + objectName, e.getMessage());
//                        //最终还是失败了
//                        mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "3", fileId, null, "处理后视频上传或入库失败");
//                    }
//                }finally {
//                    countDownLatch.countDown();
//                }
//            });
//        });
//        //等待,给一个充裕的超时时间,防止无限等待，到达超时时间还没有处理完成则结束任务
//        countDownLatch.await(30, TimeUnit.MINUTES);
//    }



}
