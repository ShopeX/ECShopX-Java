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

/** 订单商品利润分润 */
@Data
@MpTable(value = "orders_items_rel_profit", comment = "订单商品分润表", indexes = {@MpIndex(name = "idx_orderid_userid_companyid", columns = {"order_id", "user_id", "company_id"})})
public class OrderItemsProfit {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 订单id */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单id")
    private Long orderId;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", length = 64, comment = "商品id")
    private Long itemId;

    /** 分润状态：0 无效分润，1 冻结分润，2 分润成功 */
    @MpField(value = "order_profit_status", columnType = "bigint", length = 64, comment = "0 无效分润 1 冻结分润 2 分润成功")
    private Long orderProfitStatus;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 订单金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "bigint", nullable = true, comment = "订单金额，以分为单位")
    private Long totalFee;

    /** 分润类型：1 总部分润，2 自营门店分润，3 加盟门店分润 */
    @MpField(value = "profit_type", columnType = "smallint", comment = "分润类型 1 总部分润 2 自营门店分润 3 加盟门店分润")
    private Integer profitType;

    /** 购买用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "购买用户id", defaultValue = "0")
    private Long userId;

    /** 区域经销商id */
    @MpField(value = "dealer_id", columnType = "bigint", comment = "区域经销商id", defaultValue = "0")
    private Long dealerId;

    /** 拉新门店id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "拉新门店id", defaultValue = "0")
    private Long distributorId;

    /** 下单当前所在门店id */
    @MpField(value = "order_distributor_id", columnType = "bigint", comment = "下单当前所在门店id", defaultValue = "0")
    private Long orderDistributorId;

    /** 拉新导购当前所在门店id */
    @MpField(value = "distributor_nid", columnType = "bigint", comment = "拉新导购当前所在门店id", defaultValue = "0")
    private Long distributorNid;

    /** 拉新导购id */
    @MpField(value = "seller_id", columnType = "bigint", comment = "拉新导购id", defaultValue = "0")
    private Long sellerId;

    /** 推广门店id */
    @MpField(value = "popularize_distributor_id", columnType = "bigint", comment = "推广门店id", defaultValue = "0")
    private Long popularizeDistributorId;

    /** 推广导购id */
    @MpField(value = "popularize_seller_id", columnType = "bigint", comment = "推广导购id", defaultValue = "0")
    private Long popularizeSellerId;

    /** 判断拉新门店：0 无门店，1 自营门店，2 加盟门店 */
    @MpField(value = "proprietary", columnType = "bigint", comment = "判断拉新门店 0 无门店 1 自营门店 2 加盟门店", defaultValue = "0")
    private Long proprietary;

    /** 判断推广门店：0 无门店，1 自营门店，2 加盟门店 */
    @MpField(value = "popularize_proprietary", columnType = "bigint", comment = "判断推广门店 0 无门店 1 自营门店 2 加盟门店", defaultValue = "0")
    private Long popularizeProprietary;

    /** 区域经销商分成 */
    @MpField(value = "dealers", columnType = "bigint", comment = "区域经销商分成", defaultValue = "0")
    private Long dealers;

    /** 拉新门店分成 */
    @MpField(value = "distributor", columnType = "bigint", comment = "拉新门店分成", defaultValue = "0")
    private Long distributor;

    /** 拉新导购分成（分给门店） */
    @MpField(value = "seller", columnType = "bigint", comment = "拉新导购分成（分给门店）", defaultValue = "0")
    private Long seller;

    /** 推广门店分成 */
    @MpField(value = "popularize_distributor", columnType = "bigint", comment = "推广门店分成", defaultValue = "0")
    private Long popularizeDistributor;

    /** 推广导购分成（分给门店） */
    @MpField(value = "popularize_seller", columnType = "bigint", comment = "推广导购分成（分给门店）", defaultValue = "0")
    private Long popularizeSeller;

    /** 总部手续费 */
    @MpField(value = "commission", columnType = "bigint", comment = "总部手续费", defaultValue = "0")
    private Long commission;

    /** 分润规则 */
    @MpField(value = "rule", columnType = "json_array", nullable = true, comment = "分润规则")
    private String rule;
}
