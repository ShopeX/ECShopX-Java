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
 * 售后明细子表
 */
@Data
@MpTable(value = "aftersales_detail", comment = "售后明细子表", indexes = {@MpIndex(name = "idx_aftersales_bn", columns = {"aftersales_bn"}), @MpIndex(name = "idx_order_id", columns = {"order_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_company_order_sub", columns = {"company_id", "order_id", "sub_order_id"})})
public class AftersalesDetail {

    /** 售后明细ID */
    @MpId(value = "detail_id", type = IdType.AUTO, columnType = "bigint", comment = "售后明细ID")
    private Long detailId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 售后单号 */
    @MpField(value = "aftersales_bn", columnType = "bigint", comment = "售后单号")
    private Long aftersalesBn;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "string", length = 64, comment = "订单号")
    private String orderId;

    /** 订单明细表id */
    @MpField(value = "sub_order_id", columnType = "bigint", comment = "订单明细表id")
    private Long subOrderId;

    /** 产品id */
    @MpField(value = "goods_id", columnType = "bigint", comment = "产品id")
    private Long goodsId;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 商品编码 */
    @MpField(value = "item_bn", columnType = "string", nullable = true, comment = "商品编码")
    private String itemBn;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", nullable = true, comment = "商品名称")
    private String itemName;

    /**
     * 订单商品类型。normal 正常商品；gift 赠品；plus_buy 加价购商品。默认 normal
     */
    @MpField(value = "order_item_type", columnType = "string", comment = "订单商品类型,normal:正常商品，gift: 赠品, plus_buy: 加价购商品", defaultValue = "normal")
    private String orderItemType = "normal";

    /** 商品图片 */
    @MpField(value = "item_pic", columnType = "string", nullable = true, comment = "商品图片")
    private String itemPic;

    /** 售后数量 */
    @MpField(value = "num", columnType = "integer", comment = "售后数量")
    private Integer num;

    /** 应退总金额，单位(分) */
    @MpField(value = "refund_fee", columnType = "integer", comment = "应退总金额，单位(分)")
    private Integer refundFee;

    /** 积分支付应退款积分 */
    @MpField(value = "refund_point", columnType = "integer", nullable = true, comment = "积分支付应退款积分", defaultValue = "0")
    private Integer refundPoint = 0;

    /**
     * 售后服务类型。ONLY_REFUND 仅退款；REFUND_GOODS 退货退款；EXCHANGING_GOODS 换货
     */
    @MpField(value = "aftersales_type", columnType = "string", comment = "售后服务类型")
    private String aftersalesType;

    /**
     * 处理进度。0 等待商家处理；1 商家接受申请，等待消费者回寄；2 消费者回寄，等待商家收货确认（换货）；8 商家确认收货，等待审核退款；3 已驳回；4 已处理；7 已撤销（已关闭）；9 退款处理中；5 退款驳回；6 退款完成
     */
    @MpField(value = "progress", columnType = "integer", comment = "处理进度", defaultValue = "0")
    private Integer progress = 0;

    /**
     * 售后状态（明细侧语义）。0 待处理（申请中）；5 审核中；1 处理中（接受申请）；2 已处理（已完成）；3 已驳回（已拒绝）；4 已撤销（已关闭，取消售后）
     */
    @MpField(value = "aftersales_status", columnType = "integer", comment = "售后状态", defaultValue = "0")
    private Integer aftersalesStatus = 0;

    /** 创建时间（整型时间戳） */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;

    /** 售后自动驳回时间，字符串存储，默认 0 */
    @MpField(value = "auto_refuse_time", columnType = "string", comment = "售后自动驳回时间", defaultValue = "0")
    private String autoRefuseTime = "0";

    /** 实际入库数量，无符号，可为空，默认 0 */
    @MpField(value = "refunded_num", columnType = "integer", nullable = true, comment = "实际入库数量", defaultValue = "0")
    private Integer refundedNum = 0;
}
