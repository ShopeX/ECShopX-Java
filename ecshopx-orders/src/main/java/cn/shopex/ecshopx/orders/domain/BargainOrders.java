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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 砍价订单表 */
@Data
@MpTable(value = "orders_bargain", comment = "砍价订单表")
public class BargainOrders {

    /** 订单号 */
    @MpId(value = "order_id", type = IdType.INPUT, columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 订单标题 */
    @MpField(value = "title", columnType = "string", nullable = true, comment = "订单标题")
    private String title;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 活动id */
    @MpField(value = "bargain_id", columnType = "bigint", comment = "活动id")
    private Long bargainId;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", length = 255, nullable = true, comment = "商品名称")
    private String itemName;

    /** 商品价格 */
    @MpField(value = "item_price", columnType = "string", comment = "商品价格")
    private String itemPrice;

    /** 商品图片 */
    @MpField(value = "item_pics", columnType = "string", comment = "商品图片")
    private String itemPics;

    /** 购买商品数量 */
    @MpField(value = "item_num", columnType = "bigint", comment = "购买商品数量")
    private Long itemNum;

    /** 运费模板id */
    @MpField(value = "templates_id", columnType = "integer", comment = "运费模板id", defaultValue = "0")
    private Integer templatesId;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", nullable = true, comment = "手机号")
    private String mobile;

    /** 运费价格，以分为单位 */
    @MpField(value = "freight_fee", columnType = "integer", nullable = true, comment = "运费价格，以分为单位", defaultValue = "0")
    private Integer freightFee = 0;

    /** 商品金额，以分为单位 */
    @MpField(value = "item_fee", columnType = "string", comment = "商品金额，以分为单位")
    private String itemFee;

    /** 订单金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "bigint", comment = "订单金额，以分为单位")
    private Long totalFee;

    /** 订单状态 DONE—订单完成 NOTPAY—未支付 CANCEL—已取消 */
    @MpField(value = "order_status", columnType = "string", comment = "订单状态")
    private String orderStatus;

    /** 订单类型 */
    @MpField(value = "order_type", columnType = "string", comment = "订单类型", defaultValue = "bargain")
    private String orderType = "bargain";

    /** 订单来源 */
    @MpField(value = "trade_source", columnType = "string", nullable = true, comment = "订单来源")
    private String orderSource;

    /** 收货人姓名 */
    @MpField(value = "receiver_name", columnType = "string", nullable = true, comment = "收货人姓名")
    private String receiverName;

    /** 收货人手机号 */
    @MpField(value = "receiver_mobile", columnType = "string", nullable = true, comment = "收货人手机号")
    private String receiverMobile;

    /** 收货人邮编 */
    @MpField(value = "receiver_zip", columnType = "string", nullable = true, comment = "收货人邮编")
    private String receiverZip;

    /** 收货人所在省份 */
    @MpField(value = "receiver_state", columnType = "string", nullable = true, comment = "收货人所在省份")
    private String receiverState;

    /** 收货人所在城市 */
    @MpField(value = "receiver_city", columnType = "string", nullable = true, comment = "收货人所在城市")
    private String receiverCity;

    /** 收货人所在地区 */
    @MpField(value = "receiver_district", columnType = "string", nullable = true, comment = "收货人所在地区")
    private String receiverDistrict;

    /** 收货人详细地址 */
    @MpField(value = "receiver_address", columnType = "string", nullable = true, comment = "收货人详细地址")
    private String receiverAddress;

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单更新时间")
    private Integer updateTime;

    /** 订单自动取消时间 */
    @MpField(value = "auto_cancel_time", columnType = "string", comment = "订单自动取消时间")
    private String autoCancelTime;

    /** 订单来源id */
    @MpField(value = "source_id", columnType = "bigint", nullable = true, comment = "订单来源id")
    private Long sourceId;

    /** 订单监控页面id */
    @MpField(value = "monitor_id", columnType = "bigint", nullable = true, comment = "订单监控页面id")
    private Long monitorId;

    /** 订单备注 */
    @MpField(value = "remark", columnType = "string", comment = "订单备注")
    private String remark;

    /** 会员折扣金额，以分为单位 */
    @MpField(value = "member_discount", columnType = "integer", comment = "会员折扣金额，以分为单位")
    private Integer memberDiscount = 0;

    /** 优惠券抵扣金额，以分为单位 */
    @MpField(value = "coupon_discount", columnType = "integer", comment = "优惠券抵扣金额，以分为单位")
    private Integer couponDiscount = 0;

    /** 优惠券使用详情 */
    @MpField(value = "coupon_discount_desc", columnType = "text", nullable = true, comment = "优惠券使用详情")
    private String couponDiscountDesc = "";

    /** 会员折扣使用详情 */
    @MpField(value = "member_discount_desc", columnType = "text", nullable = true, comment = "会员折扣使用详情")
    private String memberDiscountDesc = "";

    /** 货币类型 */
    @MpField(value = "fee_type", columnType = "string", length = 5, comment = "货币类型", defaultValue = "CNY")
    private String feeType = "CNY";

    /** 货币汇率 */
    @MpField(value = "fee_rate", columnType = "float", precision = 15, scale = 4, comment = "货币汇率", defaultValue = "1")
    private Float feeRate = 1.0f;

    /** 货币符号 */
    @MpField(value = "fee_symbol", columnType = "string", comment = "货币符号", defaultValue = "￥")
    private String feeSymbol = "￥";

    /** 第三方特殊字段存储 */
    @MpField(value = "third_params", columnType = "json_array", nullable = true, comment = "第三方特殊字段存储")
    private String thirdParams;
}
