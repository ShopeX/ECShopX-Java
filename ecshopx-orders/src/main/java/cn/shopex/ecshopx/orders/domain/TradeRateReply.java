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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 评价回复 */
@Data
@MpTable(value = "trade_rate_reply", comment = "订单评价回复/评论表", indexes = {@MpIndex(name = "idx_rate_id", columns = {"rate_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"})})
public class TradeRateReply {

    /** 回复id */
    @MpId(value = "reply_id", type = IdType.AUTO, columnType = "bigint", comment = "回复id")
    private Long replyId;

    /** 评价id */
    @MpField(value = "rate_id", columnType = "bigint", comment = "评价id")
    private Long rateId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 操作员的id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "操作员的id")
    private Long operatorId;

    /** 评价内容 */
    @MpField(value = "content", columnType = "string", nullable = true, comment = "评价内容")
    private String content;

    /** 评价内容长度 */
    @MpField(value = "content_len", columnType = "integer", nullable = true, comment = "评价内容长度", defaultValue = "0")
    private Integer contentLen;

    /** 回复角色：seller 卖家，buyer 买家 */
    @MpField(value = "role", columnType = "string", length = 25, nullable = true, comment = "回复角色.seller：卖家；buyer：买家")
    private String role;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;

    /** 微信 unionid */
    @MpField(value = "unionid", columnType = "string", length = 60, nullable = true, comment = "微信unionid")
    private String unionid;
}
