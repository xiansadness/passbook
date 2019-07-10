package com.lx.passbook.service;

import com.lx.passbook.vo.GainPassTemplateRequest;
import com.lx.passbook.vo.Response;

/**
 * 用户领取优惠券功能实现
 */

public interface IGainPassTemplateService {

    /**
     * <h2>用户领取优惠券</h2>
     * @param request {@link GainPassTemplateRequest}
     * @return {@link Response}
     * */
    Response gainPassTemplate(GainPassTemplateRequest request) throws Exception;
}
