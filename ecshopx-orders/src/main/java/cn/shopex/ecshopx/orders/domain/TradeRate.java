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

/** 订单评价表 */
@Data
@MpTable(value = "trade_rate", comment = "订单评价表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_goods_id", columns = {"goods_id"}), @MpIndex(name = "idx_order_id", columns = {"order_id"})})
public class TradeRate {

    /** 评价id */
    @MpId(value = "rate_id", type = IdType.AUTO, columnType = "bigint", comment = "评价id")
    private Long rateId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 商品ID */
    @MpField(value = "item_id", columnType = "bigint", nullable = true, comment = "商品ID")
    private Long itemId;

    /** 产品ID */
    @MpField(value = "goods_id", columnType = "bigint", nullable = true, comment = "产品ID", defaultValue = "0")
    private Long goodsId;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "string", length = 64, nullable = true, comment = "订单号")
    private String orderId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 评价图片 */
    @MpField(value = "rate_pic", columnType = "text", nullable = true, comment = "评价图片")
    private String ratePic;

    /** 评价图片数量 */
    @MpField(value = "rate_pic_num", columnType = "integer", nullable = true, comment = "评价图片数量", defaultValue = "0")
    private Integer ratePicNum;

    /** 评价内容 */
    @MpField(value = "content", columnType = "text", nullable = true, comment = "评价内容")
    private String content;

    /** 评价内容长度 */
    @MpField(value = "content_len", columnType = "integer", nullable = true, comment = "评价内容长度", defaultValue = "0")
    private Integer contentLen;

    /** 评价是否回复：0 否，1 是 */
    @MpField(value = "is_reply", columnType = "boolean", comment = "评价是否回复。0:否；1:是", defaultValue = "0")
    private Boolean isReply;

    /** 是否删除：0 否，1 是 */
    @MpField(value = "disabled", columnType = "boolean", comment = "是否删除。0:否；1:是", defaultValue = "0")
    private Boolean disabled;

    /** 是否匿名：0 否，1 是 */
    @MpField(value = "anonymous", columnType = "boolean", comment = "是否匿名。0:否；1:是", defaultValue = "0")
    private Boolean anonymous;

    /** 评价星级 */
    @MpField(value = "star", columnType = "integer", length = 1, comment = "评价星级", defaultValue = "0")
    private Integer star;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 微信 unionid */
    @MpField(value = "unionid", columnType = "string", length = 60, nullable = true, comment = "微信unionid")
    private String unionid;

    /** 商品规格描述 */
    @MpField(value = "item_spec_desc", columnType = "text", nullable = true, comment = "商品规格描述")
    private String itemSpecDesc;

    /** 订单类型。可选值有 normal:普通实体订单, pointsmall:积分商城订单 */
    @MpField(value = "order_type", columnType = "string", comment = "订单类型。可选值有 normal:普通实体订单, pointsmall:积分商城订单", defaultValue = "normal")
    private String orderType = "normal";
}
