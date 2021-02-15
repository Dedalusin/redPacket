package com.seckill.redpacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class RedpacketApplication {
    private final static Logger LOGGER = LoggerFactory.getLogger(RedpacketApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(RedpacketApplication.class, args);
        LOGGER.info("项目启动");
    }

}
