package com.seckill.redpacket.service;

import com.seckill.redpacket.model.Result;

public interface RedPacketService {
    public Result open(long redPacketId, int userId);
}
