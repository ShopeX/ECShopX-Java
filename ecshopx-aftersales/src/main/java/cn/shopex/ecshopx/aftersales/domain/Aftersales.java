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

package cn.shopex.ecshopx.aftersales.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 售后主表
 */
@Data
@MpTable(value = "aftersales", comment = "售后主表", indexes = {@MpIndex(name = "idx_aftersales_bn", columns = {"aftersales_bn"}), @MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_merchant_id", columns = {"merchant_id"})})
public class Aftersales {

    /** 售后单号 */
    @MpId(value = "aftersales_bn", type = IdType.INPUT, columnType = "bigint", comment = "售后单号")
    private Long aftersalesBn;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 导购员id，可为空，列默认 0 */
    @MpField(value = "salesman_id", columnType = "bigint", nullable = true, comment = "导购员id", defaultValue = "0")
    private Long salesmanId = 0L;

    /** 商品编码 */
    @MpField(value = "item_bn", columnType = "string", nullable = true, comment = "商品编码")
    private String itemBn;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", comment = "门店id", defaultValue = "0")
    private Long shopId = 0L;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "integer", comment = "供应商id", defaultValue = "0")
    private Integer supplierId = 0;

    /**
     * 售后服务类型。ONLY_REFUND 仅退款；REFUND_GOODS 退货退款；EXCHANGING_GOODS 换货
     */
    @MpField(value = "aftersales_type", columnType = "string", comment = "售后服务类型")
    private String aftersalesType;

    /**
     * 售后状态。0 待处理（申请中）；1 处理中（接受申请、审核中等）；2 已处理（已完成）；3 已驳回（已拒绝）；4 已撤销（已关闭，取消售后）
     */
    @MpField(value = "aftersales_status", columnType = "integer", comment = "售后状态", defaultValue = "0")
    private Integer aftersalesStatus = 0;

    /**
     * 处理进度。0 等待商家处理；1 商家接受申请，等待消费者回寄；2 消费者回寄，等待商家收货确认；8 商家确认收货，等待审核退款；3 已驳回；4 已处理；7 已撤销（已关闭）；9 退款处理中；5 退款驳回；6 退款完成
     */
    @MpField(value = "progress", columnType = "integer", comment = "处理进度", defaultValue = "0")
    private Integer progress = 0;

    /** 应退总金额，单位(分) */
    @MpField(value = "refund_fee", columnType = "integer", comment = "应退总金额，单位(分)")
    private Integer refundFee;

    /** 应退总积分 */
    @MpField(value = "refund_point", columnType = "integer", comment = "应退总积分")
    private Integer refundPoint;

    /** 申请售后原因，最长 300 */
    @MpField(value = "reason", columnType = "string", length = 300, comment = "申请售后原因")
    private String reason;

    /** 申请描述，最长 300，可为空 */
    @MpField(value = "description", columnType = "string", length = 300, nullable = true, comment = "申请描述")
    private String description;

    /** 图片凭证信息（库内为简单数组序列化形式），可为空 */
    @MpField(value = "evidence_pic", columnType = "simple_array", nullable = true, comment = "图片凭证信息")
    private String evidencePic;

    /** 拒绝原因（长文本），可为空 */
    @MpField(value = "refuse_reason", columnType = "text", nullable = true, comment = "拒绝原因")
    private String refuseReason;

    /** 售后备注，最长 300，可为空 */
    @MpField(value = "memo", columnType = "string", length = 300, nullable = true, comment = "售后备注")
    private String memo;

    /** 消费者提交退货物流信息（JSON），可为空 */
    @MpField(value = "sendback_data", columnType = "json_array", nullable = true, comment = "消费者提交退货物流信息")
    private String sendbackData;

    /** 商家重新发货物流信息（JSON），可为空 */
    @MpField(value = "sendconfirm_data", columnType = "json_array", nullable = true, comment = "商家重新发货物流信息")
    private String sendconfirmData;

    /** 百胜等第三方返回的数据，可为空 */
    @MpField(value = "third_data", columnType = "string", nullable = true, comment = "百胜等第三方返回的数据")
    private String thirdData;

    /** 售后回寄地址信息（JSON），可为空 */
    @MpField(value = "aftersales_address", columnType = "json_array", nullable = true, comment = "售后回寄地址信息")
    private String aftersalesAddress;

    /** 店务端商家备注，最长 255，非空时默认空串 */
    @MpField(value = "distributor_remark", columnType = "string", length = 255, comment = "店务端商家备注")
    private String distributorRemark = "";

    /** 创建时间（整型时间戳） */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;

    /** 联系人，最长 500，可为空 */
    @MpField(value = "contact", columnType = "string", length = 500, nullable = true, comment = "联系人")
    private String contact;

    /** 手机号，最长 255，可为空 */
    @MpField(value = "mobile", columnType = "string", length = 255, nullable = true, comment = "手机号")
    private String mobile;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;

    /**
     * 是否部分取消订单退款（库内布尔/整型标志）。false/0 否；true/1 是。可为空，列默认 0（对应 false）
     */
    @MpField(value = "is_partial_cancel", columnType = "boolean", nullable = true, comment = "是否部分取消订单退款", defaultValue = "0")
    private Boolean isPartialCancel = false;

    /**
     * 退货方式，最长 20。logistics 寄回；offline 到店退。默认 logistics
     */
    @MpField(value = "return_type", columnType = "string", length = 20, comment = "退货方式：logistics寄回 offline到店退", defaultValue = "logistics")
    private String returnType = "logistics";

    /** 退货门店ID，无符号 bigint，默认 0 */
    @MpField(value = "return_distributor_id", columnType = "bigint", comment = "退货门店ID", defaultValue = "0")
    private Long returnDistributorId = 0L;

    /** 自配送员id，可为空；列默认 0 */
    @MpField(value = "self_delivery_operator_id", columnType = "bigint", nullable = true, comment = "自配送员id", defaultValue = "0")
    private Long selfDeliveryOperatorId = 0L;

    /** 退款运费，无符号，可为空，默认 0（单位：分） */
    @MpField(value = "freight", columnType = "integer", nullable = true, comment = "退款运费", defaultValue = "0")
    private Integer freight = 0;

    /** 运费类型（积分商城等场景），最长 10。cash 现金；point 积分。默认 cash */
    @MpField(value = "freight_type", columnType = "string", length = 10, comment = "运费类型-用于积分商城 cash:现金 point:积分", defaultValue = "cash")
    private String freightType = "cash";
}
