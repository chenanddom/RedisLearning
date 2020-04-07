package com.itdom.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * @Description:一句话的功能说明
 * @Author: chendom
 * @Date: 2020/4/3 9:38
 * @Version 1.0
 */
@RestController
@RequestMapping("/set")
public class SetController {

    private static final Logger logger = LoggerFactory.getLogger(SetController.class);


    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/add")
    public void add(){
        Long addCount = redisTemplate.opsForSet().add("set", "a", "b", "c");
        logger.info("addCount:{}",addCount);
    }

    @GetMapping("/get")
    public void getElement(){
        Set set = redisTemplate.opsForSet().members("set");
        set.forEach(e->{
//            System.out.println(e.toString());
            logger.info(e.toString());
        });
    }

    @PutMapping("/update")
    public void updateElement(){
        Long remove = redisTemplate.opsForSet().remove("set", "a");
        logger.info("remomveCount:{}",remove);
    }


}
