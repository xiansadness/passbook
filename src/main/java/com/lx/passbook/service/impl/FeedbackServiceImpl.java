package com.lx.passbook.service.impl;


import com.alibaba.fastjson.JSON;
import com.lx.passbook.constant.Constants;
import com.lx.passbook.mapper.FeedbackRowMapper;
import com.lx.passbook.service.IFeedbackService;
import com.lx.passbook.utils.RowKeyGenUtil;
import com.lx.passbook.vo.Feedback;
import com.lx.passbook.vo.Response;
import com.spring4all.spring.boot.starter.hbase.api.HbaseTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.PrefixFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评论功能实现
 */

@Slf4j
@Service
public class FeedbackServiceImpl implements IFeedbackService {

    /** HBase 客户端 */
    private final HbaseTemplate hbaseTemplate;

    @Autowired
    public FeedbackServiceImpl(HbaseTemplate hbaseTemplate) {
        this.hbaseTemplate = hbaseTemplate;
    }

    @Override
    public Response createFeedback(Feedback feedback) {

        if (!feedback.validate()) {//校验feedback是否有效
            log.error("Feedback Error: {}", JSON.toJSONString(feedback));
            return Response.failure("Feedback Error");
        }

        Put put = new Put(Bytes.toBytes(RowKeyGenUtil.genFeedbackRowKey(feedback)));

        put.addColumn(
                Bytes.toBytes(Constants.Feedback.FAMILY_I),
                Bytes.toBytes(Constants.Feedback.USER_ID),
                Bytes.toBytes(feedback.getUserId())
        );
        put.addColumn(
                Bytes.toBytes(Constants.Feedback.FAMILY_I),
                Bytes.toBytes(Constants.Feedback.TYPE),
                Bytes.toBytes(feedback.getType())
        );
        put.addColumn(
                Bytes.toBytes(Constants.Feedback.FAMILY_I),
                Bytes.toBytes(Constants.Feedback.TEMPLATE_ID),
                Bytes.toBytes(feedback.getTemplateId())
        );
        put.addColumn(
                Bytes.toBytes(Constants.Feedback.FAMILY_I),
                Bytes.toBytes(Constants.Feedback.COMMENT),
                Bytes.toBytes(feedback.getComment())
        );

        hbaseTemplate.saveOrUpdate(Constants.Feedback.TABLE_NAME, put);

        return Response.success();
    }

    @Override
    public Response getFeedback(Long userId) {

        byte[] reverseUserId = new StringBuilder(String.valueOf(userId)).reverse().toString().getBytes();//可以参看feedback表，rowKey的生成规则
        Scan scan = new Scan();//对HBase表进行扫描
        scan.setFilter(new PrefixFilter(reverseUserId));//为扫描器配置过滤器

        List<Feedback> feedbacks = hbaseTemplate.find(Constants.Feedback.TABLE_NAME, scan, new FeedbackRowMapper());
        //get只能得到一条，且若获取不到会发生异常
        //find可以获取多条，若获取不到则返回null

        return new Response(feedbacks);
    }
}







