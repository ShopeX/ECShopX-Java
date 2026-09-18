/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.comments.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 店铺评论表
 */
@Data
@MpTable(value = "shop_comments", comment = "店铺评论表")
public class ShopComments {

    /** 评论id */
    @MpId(value = "item_id", type = IdType.AUTO, columnType = "bigint", comment = "评论id")
    private Long commentId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "string", comment = "公司id")
    private String companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "string", comment = "用户id")
    private String userId;

    /** 店铺id，默认 0 */
    @MpField(value = "shop_id", columnType = "string", comment = "店铺id", defaultValue = "0")
    private String shopId = "0";

    /** 评论内容，最长 2000 */
    @MpField(value = "content", columnType = "string", length = 2000, comment = "评论内容")
    private String content;

    /** 评论图片（库内为 JSON），可为空 */
    @MpField(value = "pics", columnType = "json_array", nullable = true, comment = "评论图片")
    private String pics;

    /** 评论是否回复，默认 false，可为空 */
    @MpField(value = "is_reply", columnType = "boolean", nullable = true, comment = "评论是否回复", defaultValue = "False")
    private Boolean isReply = false;

    /** 评论回复内容，最长 2000，可为空 */
    @MpField(value = "reply_content", columnType = "string", length = 2000, nullable = true, comment = "评论回复内容")
    private String replyContent;

    /** 是否置顶，默认 false */
    @MpField(value = "stuck", columnType = "boolean", comment = "是否置顶", defaultValue = "False")
    private Boolean stuck = false;

    /** 是否隐藏，默认 false */
    @MpField(value = "hid", columnType = "boolean", comment = "是否隐藏", defaultValue = "False")
    private Boolean hid = false;

    /** 回复时间，可为空 */
    @MpField(value = "reply_time", columnType = "bigint", nullable = true, comment = "回复时间")
    private Long replyTime;

    /**
     * 评论来源，可为空。常见取值：wechat（微信）、ali（支付宝）
     */
    @MpField(value = "source", columnType = "string", nullable = true, comment = "评论来源")
    private String source;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
