package com.lx.passbook.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户领取的优惠券
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pass {

    /** 用户 id */
    private Long userId;

    /** pass 在 HBase 中的 RowKey */
    private String rowKey;

    /** PassTemplate 在 HBase 中的 RowKey */
    private String templateId;//相当于是优惠券的id

    /** 优惠券 token, 有可能是 null, 则填充 -1 */
    private String token;

    /** 领取日期 */
    private Date assignedDate;

    /** 消费日期, 不为空代表已经被消费了 */
    private Date conDate;

}
