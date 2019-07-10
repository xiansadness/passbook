package com.lx.passbook.service;

import com.lx.passbook.vo.Pass;
import com.lx.passbook.vo.Response;

/**
 * 获取用户个人优惠券信息
 */

public interface IUserPassService {

    /**
     * <h2>获取用户个人优惠券信息, 即我的优惠券功能实现</h2>
     * @param userId 用户 id
     * @return {@link Response}
     * */
    //用户当前可以使用的优惠券
    Response getUserPassInfo(Long userId) throws Exception;

    /**
     * <h2>获取用户已经消费了的优惠券, 即已使用优惠券功能实现</h2>
     * @param userId 用户 id
     * @return {@link Response}
     * */
    //用户已使用过的优惠券
    Response getUserUsedPassInfo(Long userId) throws Exception;

    /**
     * <h2>获取用户所有的优惠券</h2>
     * @param userId 用户 id
     * @return {@link Response}
     * */
    //用户领取过的所有优惠券（已使用+未使用+过期）
    Response getUserAllPassInfo(Long userId) throws Exception;

    /**
     * <h2>用户使用优惠券</h2>
     * @param pass {@link Pass}
     * @return {@link Response}
     * */
    //用户对优惠券执行使用操作
    Response userUsePass(Pass pass);
}
