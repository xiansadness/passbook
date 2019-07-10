package com.lx.passbook.vo;

import com.google.common.base.Enums;
import com.lx.passbook.constant.FeedbackType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户评论
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Feedback {

    /** 用户 id */
    private Long userId;

    /** 评论类型 */
    private String type;

    /** PassTemplate RowKey, 如果是 app 类型的评论, 则没有该字段（即没有该列） */
    private String templateId;

    /** 评论内容 */
    private String comment;

    public boolean validate() {

        FeedbackType feedbackType = Enums.getIfPresent(
                FeedbackType.class, this.type.toUpperCase()
        ).orNull();//type字段跟反馈类型枚举比较

        return !(null == feedbackType || null == comment);
    }
}
