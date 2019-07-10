package com.lx.passbook.service;

import com.alibaba.fastjson.JSON;
import com.lx.passbook.constant.Constants;
import com.lx.passbook.vo.PassTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 消费 kafka 中的 PassTemplate
 * 即消费 kafka 中的优惠券
 */
@Slf4j
@Component
public class ConsumePassTemplate {

    /** pass 相关的 HBase 服务 */
    private final IHBasePassService passService;

    @Autowired
    public ConsumePassTemplate(IHBasePassService passService) {
        this.passService = passService;
    }

    @KafkaListener(topics = {Constants.TEMPLATE_TOPIC})
    public void receive(@Payload String passTemplate,
                        @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.info("Consumer Receive PassTemplate: {}", passTemplate);

        PassTemplate pt;

        try {
            pt = JSON.parseObject(passTemplate, PassTemplate.class);
        } catch (Exception ex) {
            log.error("Parse PassTemplate Error: {}", ex.getMessage());
            return;
        }

        log.info("DropPassTemplateToHBase: {}", passService.dropPassTemplateToHBase(pt));
    }
}
