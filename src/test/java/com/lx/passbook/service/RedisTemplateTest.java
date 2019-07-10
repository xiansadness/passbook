package com.lx.passbook.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * redis客户端测试
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisTemplateTest {

    //redis客户端
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    public void testRedisTemplate(){
        //清空redis
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.flushAll();
            return null;
        });

        assert redisTemplate.opsForValue().get("name") == null;

        redisTemplate.opsForValue().set("name","whale");
        assert  redisTemplate.opsForValue().get("name") != null;

        System.out.println(redisTemplate.opsForValue().get("name"));

    }




}
