package com.lx.passbook.service;

import com.alibaba.fastjson.JSON;
import com.lx.passbook.vo.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 用户服务测试
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServiceTest {

    @Autowired
    private IUserService userService;

    @Test
    public void testCreateUser() throws Exception {

        User user = new User();
        user.setBaseInfo(
                new User.BaseInfo("spring", 20, "w")
        );
        user.setOtherInfo(
                new User.OtherInfo("12378945625", "南京市")
        );

        System.out.println(JSON.toJSONString(userService.createUser(user)));

        //{
        //      "data":{
        //          "baseInfo":{"age":20,"name":"spring","sex":"w"},
        //          "id":119866,
        //          "otherInfo":{"address":"南京市","phone":"12378945625"}
        //       },
        //      "errorCode":0,
        //      "errorMsg":""}
    }


}
