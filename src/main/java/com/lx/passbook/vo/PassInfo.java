package com.lx.passbook.vo;

import com.lx.passbook.entity.Merchants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户领取的优惠券信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassInfo {

    /** 优惠券 */
    //用户所拥有的一张优惠券
    private Pass pass;

    /** 优惠券模板 */
    //该优惠券本身的详细信息
    private PassTemplate passTemplate;

    /** 优惠券对应的商户 */
    //该优惠券对应商户的详细信息
    private Merchants merchants;
}
