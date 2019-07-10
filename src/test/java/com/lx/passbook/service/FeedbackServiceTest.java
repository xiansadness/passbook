package com.lx.passbook.service;

import com.alibaba.fastjson.JSON;
import com.lx.passbook.constant.FeedbackType;
import com.lx.passbook.vo.Feedback;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 用户反馈服务测试
 *
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class FeedbackServiceTest extends AbstractServiceTest {

    @Autowired
    private IFeedbackService feedbackService;

    @Test
    public void testCreateFeedback() {

        Feedback appFeedback = new Feedback();
        appFeedback.setUserId(userId);
        appFeedback.setType(FeedbackType.APP.getCode());
        appFeedback.setTemplateId("-1");
        appFeedback.setComment("passbook卡包评论");

        System.out.println(JSON.toJSONString(
                feedbackService.createFeedback(appFeedback))
        );

        Feedback passFeedback = new Feedback();
        passFeedback.setUserId(userId);
        passFeedback.setType(FeedbackType.PASS.getCode());
        passFeedback.setTemplateId("d8314deb84572f97f052834245b37136");
        passFeedback.setComment("passtemplate优惠券评论");

        System.out.println(JSON.toJSONString(
                feedbackService.createFeedback(passFeedback)
        ));

    }


    @Test
    public void testGetFeedback() {

        System.out.println(JSON.toJSONString(
                feedbackService.getFeedback(userId))
        );

        //{
        //  "data":[
        //      {"comment":"passtemplate优惠券评论","templateId":"d8314deb84572f97f052834245b37136",
        //              "type":"pass","userId":119866},
        //      {"comment":"passbook卡包评论","templateId":"-1","type":"app","userId":119866}],
        //  "errorCode":0,
        //  "errorMsg":""}
    }
}
