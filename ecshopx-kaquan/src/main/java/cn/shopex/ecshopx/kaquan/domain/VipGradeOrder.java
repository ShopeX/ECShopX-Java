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

package cn.shopex.ecshopx.kaquan.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 付费会员等级卡 */
@Data
@MpTable(value = "kaquan_vip_grade_order", comment = "付费会员等级卡")
public class VipGradeOrder {

    /** 订单号 */
    @MpId(value = "order_id", type = IdType.INPUT, columnType = "bigint", comment = "订单号")
    private Long orderId;

    /** 付费会员卡等级ID */
    @MpField(value = "vip_grade_id", columnType = "integer", comment = "付费会员卡等级ID")
    private Integer vipGradeId;

    /** 等级类型 */
    @MpField(value = "lv_type", columnType = "string", comment = "等级类型")
    private String lvType;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "integer", comment = "公司ID")
    private Integer companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 用户手机号 */
    @MpField(value = "mobile", columnType = "string", nullable = true, comment = "用户手机号")
    private String mobile;

    /** 会员卡标题 */
    @MpField(value = "title", columnType = "string", nullable = true, comment = "会员卡标题")
    private String title;

    /** 付款金额 */
    @MpField(value = "price", columnType = "integer", comment = "付款金额")
    private Integer price;

    /** 购买的卡片类型,可选值有  monthly:30天月度卡;quarter:30天季度卡;year:365天年度卡（JSON） */
    @MpField(value = "card_type", columnType = "json_array", comment = "购买的卡片类型,可选值有  monthly:30天月度卡;quarter:30天季度卡;year:365天年度卡")
    private String cardType;

    /** 折扣额度 */
    @MpField(value = "discount", columnType = "integer", comment = "折扣额度", defaultValue = "0")
    private Integer discount = 0;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", comment = "门店id", defaultValue = "0")
    private Long shopId = 0L;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 订单来源id */
    @MpField(value = "source_id", columnType = "bigint", nullable = true, comment = "订单来源id")
    private Long sourceId;

    /** 订单监控页面id */
    @MpField(value = "monitor_id", columnType = "bigint", nullable = true, comment = "订单监控页面id")
    private Long monitorId;

    /** 订单状态,可选值有 DONE—订单完成;NOTPAY—未支付;CANCEL—已取消 */
    @MpField(value = "order_status", columnType = "string", nullable = true, comment = "订单状态,可选值有 DONE—订单完成;NOTPAY—未支付;CANCEL—已取消")
    private String orderStatus;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;

    /** 货币类型 */
    @MpField(value = "fee_type", columnType = "string", length = 5, comment = "货币类型", defaultValue = "CNY")
    private String feeType = "CNY";

    /** 货币汇率 */
    @MpField(value = "fee_rate", columnType = "float", precision = 15, scale = 4, comment = "货币汇率", defaultValue = "1")
    private Double feeRate = 1.0;

    /** 货币符号 */
    @MpField(value = "fee_symbol", columnType = "string", comment = "货币符号", defaultValue = "￥")
    private String feeSymbol = "￥";

    /** 订单来源,可选值有 sale:购买;receive:领取;admin:后台手动续期;gift:赠送 */
    @MpField(value = "source_type", columnType = "string", comment = "订单来源,可选值有 sale:购买;receive:领取;admin:后台手动续期;gift:赠送", defaultValue = "sale")
    private String sourceType = "sale";
}
