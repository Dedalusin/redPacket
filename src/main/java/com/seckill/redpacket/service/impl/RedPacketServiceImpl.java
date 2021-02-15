package com.seckill.redpacket.service.impl;

import com.seckill.redpacket.dao.DynamicQuery;
import com.seckill.redpacket.model.RedPacketRecord;
import com.seckill.redpacket.model.Result;
import com.seckill.redpacket.service.RedPacketService;
import com.seckill.redpacket.utils.RedisUtil;
import com.seckill.redpacket.utils.RedissLockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Random;

@Service
public class RedPacketServiceImpl implements RedPacketService {

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    DynamicQuery dynamicQuery;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result open(long redPacketId, int userId) {
        Integer money = 0;
        boolean res = false;
        try {
            res = RedissLockUtil.tryLock(redPacketId+"",3,10);
            if (res){
                //获取到锁
                long restPacket = redisUtil.decr(redPacketId+"-num",1);
                if (restPacket == 0){
                    //最后一个红包,得到剩余所有钱,且因为已经没有红包了所以也无需对money进行减少
                    money = (Integer) redisUtil.getValue(redPacketId+"-money");
                    save(money, redPacketId, userId);
                }else if (restPacket < 0){
                    return Result.error("手慢力");
                }else {
                    Random random = new Random();
                    Integer restMoney = (Integer) redisUtil.getValue(redPacketId+"-money");
                    //random.nextInt产生[1,剩余人均钱财的两倍)
                    money = random.nextInt(money = random.nextInt((int) ( restMoney/ (restPacket+1) * 2 - 1))) + 1;
                    redisUtil.decr(redPacketId+"-money",money);
                    save(money, redPacketId, userId);
                }
            }
        }catch (Exception e){
                e.printStackTrace();
        }finally {
            //解锁
            if (res){
                RedissLockUtil.unlock(redPacketId+"");
            }
        }
        return Result.ok("抢到了: "+ money + "分钱");
    }
    @Async
    void save(int money, long redPacketId, int userId){
        RedPacketRecord record = new RedPacketRecord();
        record.setMoney(money);
        record.setRedPacketId(redPacketId);
        record.setUid(userId);
        record.setCreateTime(new Timestamp(System.currentTimeMillis()));
        dynamicQuery.save(record);
    }
}
