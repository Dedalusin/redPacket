package com.seckill.redpacket.controller;

import com.seckill.redpacket.model.Result;
import com.seckill.redpacket.service.RedPacketService;
import com.seckill.redpacket.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Api("抢红包")
@RestController
@RequestMapping("/redPacket")
public class RedPacketController {
    //初始化线程池
    private static int coreThread = Runtime.getRuntime().availableProcessors();
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(coreThread,coreThread+1, 10L, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(200));
    private final static Logger LOGGER = LoggerFactory.getLogger(RedPacketController.class);
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    RedPacketService redPacketService;

    @ApiOperation(value = "抢红包")
    @PostMapping("/start")
    public Result start(long redPacketId){
        int people = 100;
        //信号量,运行一个线程减1，到0运行await阻塞的
        final CountDownLatch latch = new CountDownLatch(people);
        //初始化红包
        redisUtil.cacheValue(redPacketId+"-num",10);
        redisUtil.cacheValue(redPacketId+"-money",30000);
        /**
         * 模拟用户抢红包
         */
        for (int i=1; i <= people; i++){
            int userId = i;
            Runnable task = () -> {
                Integer restMoney = (Integer) redisUtil.getValue(redPacketId+"-money");
                if (restMoney > 0){
                    //抢到，可能有多个人同时抢到红包，但不一定能抢先拆到
                    //进行业务层操作
                    Result result = redPacketService.open(redPacketId, userId);
                    if (result.get("code").toString().equals("500")){
                        //没抢到
                        LOGGER.info("这位"+userId+"同志运气真差，没有抢到");
                    }else {
                        LOGGER.info("恭喜"+userId+"同志, "+ result.get("msg"));
                    }
                }
            };
            //提交任务至线程池
            executor.execute(task);
        }
        try {
            latch.await();
            Integer restMoney = Integer.parseInt(redisUtil.getValue(redPacketId+"-money").toString());
            LOGGER.info("剩余金额：{}",restMoney);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Result.ok();
    }
}
