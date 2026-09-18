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

package cn.shopex.ecshopx.chinaumspay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 银联商务支付分账流水明细
 */
@Data
@MpTable(value = "chinaumspay_division_detail", comment = "银联商务支付分账流水明细", indexes = {@MpIndex(name = "idx_company", columns = {"company_id"}), @MpIndex(name = "idx_division_id", columns = {"division_id"})})
public class ChinaumspayDivisionDetail {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 分账流水ID */
    @MpField(value = "division_id", columnType = "bigint", comment = "分账流水ID")
    private Long divisionId;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", comment = "订单号")
    private Long orderId;

    /** 店铺ID */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺ID")
    private Long distributorId;

    /** 订单金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "string", comment = "订单金额，以分为单位")
    private String totalFee;

    /** 订单实际金额，以分为单位 */
    @MpField(value = "actual_fee", columnType = "string", comment = "订单实际金额，以分为单位")
    private String actualFee;

    /** 收单手续费费率 */
    @MpField(value = "commission_rate", columnType = "float", precision = 15, scale = 4, comment = "收单手续费费率")
    private Double commissionRate;

    /** 收单手续费金额，以分为单位 */
    @MpField(value = "commission_rate_fee", columnType = "integer", comment = "收单手续费金额，以分为单位", defaultValue = "0")
    private Integer commissionRateFee = 0;

    /** 分账金额，以分为单位 */
    @MpField(value = "division_fee", columnType = "integer", comment = "分账金额，以分为单位", defaultValue = "0")
    private Integer divisionFee = 0;

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单更新时间")
    private Integer updateTime;
}
