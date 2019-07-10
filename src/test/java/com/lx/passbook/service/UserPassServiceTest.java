package com.lx.passbook.service;

import com.alibaba.fastjson.JSON;
import com.lx.passbook.vo.Pass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UserPassServiceTest extends AbstractServiceTest {

    @Autowired
    private IUserPassService userPassService;

    //{
    //      "data":[
    //          {"merchants":{"address":"Shanghai","businessLicenseUrl":"www.baidu.com",
    //              "id":5,"isAudit":true,"logoUrl":"www.baidu.com","name":"whale","phone":"1244567890"},
    //           "pass":{"assignedDate":1547049600000,"rowKey":"6689119223370474095119251d8314deb84572f97f052834245b37136",
    //              "templateId":"d8314deb84572f97f052834245b37136","token":"token-4","userId":119866},
    //           "passTemplate":{"background":2,"desc":"detail: whale-2","end":1563552000000,
    //              "hasToken":true,"id":5,"limit":9999,"start":1562688000000,
    //                  "summary":"summary: whale-2","title":"title: whale-2"}}],
    //      "errorCode":0,
    //      "errorMsg":""
    //}
    @Test
    public void testGetUserPassInfo() throws Exception {

        System.out.println(JSON.toJSONString(
                userPassService.getUserPassInfo(userId))
        );
    }

    @Test
    public void testGetUserUsedPassInfo() throws Exception {

        System.out.println(JSON.toJSONString(
                userPassService.getUserUsedPassInfo(userId)
        ));
    }

    @Test
    public void testGetUserAllPassInfo() throws Exception {

        System.out.println(JSON.toJSONString(
                userPassService.getUserAllPassInfo(userId)
        ));
    }

    @Test
    public void testUserUsePass() {

        Pass pass = new Pass();
        pass.setUserId(userId);
        pass.setTemplateId("d8314deb84572f97f052834245b37136");

        System.out.println(JSON.toJSONString(
                userPassService.userUsePass(pass)
        ));
    }
}
