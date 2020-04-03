package com.itdom.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.web.bind.annotation.*;

/**
 * @Description:一句话的功能说明
 * @Author: chendom
 * @Date: 2020/4/2 9:42
 * @Version 1.0
 */
@RestController
@RequestMapping("/hash")
public class HashController {
    private static final Logger logger = LoggerFactory.getLogger(HashController.class);
    @Autowired
    private RedisTemplate redisTemplate;


    @PostMapping("/addHashValue")
    public void addHashValue(){
        logger.info("add value before");
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer(String.class));
        redisTemplate.opsForHash().put("person","name","hello");
        logger.info("add value after");
        Object o = redisTemplate.opsForHash().get("person", "name");
        logger.info("the value is {}",o);

    }
    @GetMapping("/getHashValue")
    public void getHashValue(){
        Object o = redisTemplate.opsForHash().get("person", "name");
        logger.info("return value is {}",o);
        Object o1 = redisTemplate.boundHashOps("person").get("name");
        logger.info("mothod2 return value is {}",o);
    }

    @DeleteMapping("/deleteHashValue")
    public void deleteHashValue(){

        Long delete = redisTemplate.opsForHash().delete("person", "name");
        logger.info("deleteCount:{}",delete);
    }
}
