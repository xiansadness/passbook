package com.lx.passbook.service;

import com.lx.passbook.vo.PassTemplate;

/**
 * Pass Hbase 服务
 */

public interface IHBasePassService {

    /**
     * <h2>将 PassTemplate 写入 HBase</h2>
     * @param passTemplate {@link PassTemplate}
     * @return true/false
     * */
    //商户投放优惠券——>kafka——>系统从kafka中取商户投放的优惠券(消费优惠券)——>将取出来的优惠券存放到HBase中
    boolean dropPassTemplateToHBase(PassTemplate passTemplate);
}
