package com.lx.passbook.service;

import com.lx.passbook.vo.Response;

/**
 * 获取库存信息：只返回用户没有领取的，即优惠券库存功能实现的接口定义
 */

public interface IInventoryService {

    /**
     * <h2>获取库存信息</h2>
     * @param userId 用户 id
     * @return {@link Response}
     * */
    Response getInventoryInfo(Long userId) throws Exception;


}
