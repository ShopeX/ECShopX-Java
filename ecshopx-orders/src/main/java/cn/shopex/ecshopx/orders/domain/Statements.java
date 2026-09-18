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

/** 结算单 */
@Data
@MpTable(value = "statements", comment = "结算单", indexes = {@MpIndex(name = "idx_merchant_type", columns = {"merchant_type"}), @MpIndex(name = "idx_supplier_id", columns = {"supplier_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "idx_start_time", columns = {"start_time"})})
public class Statements {

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

    /** 商户类型：distributor 经销商,supplier 供应商 */
    @MpField(value = "merchant_type", columnType = "string", length = 30, nullable = true, comment = "商户类型：distributor 经销商,supplier 供应商", defaultValue = "distributor")
    private String merchantType = "distributor";

    /** 结算单号 */
    @MpField(value = "statement_no", columnType = "string", length = 20, comment = "结算单号")
    private String statementNo;

    /** 订单数量 */
    @MpField(value = "order_num", columnType = "integer", comment = "订单数量")
    private Integer orderNum;

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

    /** 结算周期开始时间 */
    @MpField(value = "start_time", columnType = "integer", comment = "结算周期开始时间")
    private Integer startTime;

    /** 结算周期结束时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "结算周期结束时间")
    private Integer endTime;

    /** 确认时间 */
    @MpField(value = "confirm_time", columnType = "integer", nullable = true, comment = "确认时间")
    private Integer confirmTime;

    /** 结算时间 */
    @MpField(value = "statement_time", columnType = "integer", nullable = true, comment = "结算时间")
    private Integer statementTime;

    /** 结算状态 ready:待商家确认 confirmed待平台结算 done:已结算 */
    @MpField(value = "statement_status", columnType = "string", comment = "结算状态 ready:待商家确认 confirmed待平台结算 done:已结算", defaultValue = "ready")
    private String statementStatus = "ready";

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

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
