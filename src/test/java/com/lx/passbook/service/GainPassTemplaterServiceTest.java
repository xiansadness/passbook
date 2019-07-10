package com.lx.passbook.service;

import com.alibaba.fastjson.JSON;
import com.lx.passbook.vo.GainPassTemplateRequest;
import com.lx.passbook.vo.PassTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 用户领取优惠券功能测试
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class GainPassTemplaterServiceTest extends AbstractServiceTest {

    @Autowired
    private IGainPassTemplateService gainPassTemplateService;

    @Test
    public void testGainPassTemplate() throws Exception {

        PassTemplate target = new PassTemplate();
        target.setId(5);
        target.setTitle("title: whale-2");
        target.setHasToken(true);

        System.out.println(JSON.toJSONString(
                gainPassTemplateService.gainPassTemplate(
                        new GainPassTemplateRequest(userId, target)
                )
        ));
    }
}
