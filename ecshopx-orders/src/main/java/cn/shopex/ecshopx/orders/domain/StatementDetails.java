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

/** 结算单明细 */
@Data
@MpTable(value = "statement_details", comment = "结算单明细", indexes = {@MpIndex(name = "idx_statement_id", columns = {"statement_id"}), @MpIndex(name = "idx_supplier_id", columns = {"supplier_id"}), @MpIndex(name = "idx_created", columns = {"created"})})
public class StatementDetails {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id")
    private Long merchantId;

    /** 供应商ID */
    @MpField(value = "supplier_id", columnType = "bigint", comment = "供应商ID", defaultValue = "0")
    private Long supplierId = 0L;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id")
    private Long distributorId;

    /** 结算单ID */
    @MpField(value = "statement_id", columnType = "bigint", comment = "结算单ID")
    private Long statementId;

    /** 结算单号 */
    @MpField(value = "statement_no", columnType = "string", length = 20, comment = "结算单号")
    private String statementNo;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 实付金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "integer", comment = "实付金额，以分为单位")
    private Integer totalFee;

    /** 运费金额，以分为单位 */
    @MpField(value = "freight_fee", columnType = "integer", comment = "运费金额，以分为单位")
    private Integer freightFee;

    /** 同城配金额，以分为单位 */
    @MpField(value = "intra_city_freight_fee", columnType = "integer", comment = "同城配金额，以分为单位")
    private Integer intraCityFreightFee;

    /** 分销佣金，以分为单位 */
    @MpField(value = "rebate_fee", columnType = "integer", comment = "分销佣金，以分为单位")
    private Integer rebateFee;

    /** 退款金额，以分为单位 */
    @MpField(value = "refund_fee", columnType = "integer", comment = "退款金额，以分为单位")
    private Integer refundFee;

    /** 结算金额，以分为单位 */
    @MpField(value = "statement_fee", columnType = "integer", comment = "结算金额，以分为单位")
    private Integer statementFee;

    /** 支付方式 */
    @MpField(value = "pay_type", columnType = "string", comment = "支付方式")
    private String payType;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 购买数量 */
    @MpField(value = "num", columnType = "integer", nullable = true, comment = "购买数量")
    private Integer num = 0;

    /** 销售总金额，以分为单位 */
    @MpField(value = "item_fee", columnType = "integer", nullable = true, comment = "销售总金额，以分为单位")
    private Integer itemFee = 0;

    /** 佣金金额，以分为单位 */
    @MpField(value = "commission_fee", columnType = "integer", nullable = true, comment = "佣金金额，以分为单位")
    private Integer commissionFee = 0;

    /** 成本结算金额，以分为单位 */
    @MpField(value = "cost_fee", columnType = "integer", nullable = true, comment = "结算金额，以分为单位")
    private Integer costFee = 0;

    /** 积分抵扣 按分计算 */
    @MpField(value = "point_fee", columnType = "integer", nullable = true, comment = "积分抵扣 按分计算")
    private Integer pointFee = 0;

    /** 退货数量 */
    @MpField(value = "refund_num", columnType = "integer", nullable = true, comment = "退货数量")
    private Integer refundNum = 0;

    /** 退款积分 */
    @MpField(value = "refund_point", columnType = "integer", nullable = true, comment = "退款积分")
    private Integer refundPoint = 0;

    /** 退货成本 */
    @MpField(value = "refund_cost_fee", columnType = "integer", nullable = true, comment = "退货成本")
    private Integer refundCostFee = 0;
}
