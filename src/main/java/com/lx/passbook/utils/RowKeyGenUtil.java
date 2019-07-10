package com.lx.passbook.utils;

import com.lx.passbook.vo.Feedback;
import com.lx.passbook.vo.GainPassTemplateRequest;
import com.lx.passbook.vo.PassTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

@Slf4j
public class RowKeyGenUtil {

    /**
     * <h2>根据提供的 PassTemplate 对象生成 RowKey</h2>
     * @param passTemplate {@link PassTemplate}
     * @return String RowKey
     * */
    public static String genPassTemplateRowKey(PassTemplate passTemplate) {

        String passInfo = String.valueOf(passTemplate.getId()) + "_" + passTemplate.getTitle();
        String rowKey = DigestUtils.md5Hex(passInfo);
        log.info("GenPassTemplateRowKey: {}, {}", passInfo, rowKey);

        return rowKey;
    }

    /**
     * <h2>根据 Feedback 构造 RowKey</h2>
     * @param feedback {@link Feedback}
     * @return String RowKey
     * */
    public static String genFeedbackRowKey(Feedback feedback) {

        return new StringBuilder(String.valueOf(feedback.getUserId())).reverse().toString() +
                (Long.MAX_VALUE - System.currentTimeMillis());
    }

    /**
     * <h2>根据提供的领取优惠券请求生成 RowKey, 只可以在领取优惠券的时候使用</h2>
     * Pass RowKey = reversed(userId) + inverse(timestamp) + PassTemplate RowKey
     * 翻转userId + (Long.MAX_VALUE-当前时间戳) + PassTemplate RowKey
     * @param request {@link GainPassTemplateRequest}
     * @return String RowKey
     * */
    public static String genPassRowKey(GainPassTemplateRequest request) {

        return new StringBuilder(String.valueOf(request.getUserId())).reverse().toString()
                + (Long.MAX_VALUE - System.currentTimeMillis())
                + genPassTemplateRowKey(request.getPassTemplate());
    }


}
