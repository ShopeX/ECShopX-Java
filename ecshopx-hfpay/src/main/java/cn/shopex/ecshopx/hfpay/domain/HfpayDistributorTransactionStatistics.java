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

package cn.shopex.ecshopx.hfpay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 店铺交易统计统计
 */
@Data
@MpTable(value = "hfpay_distributor_transaction_statistics", comment = "店铺交易统计统计", indexes = {@MpIndex(name = "idx_company_id_distributor_id", columns = {"company_id", "distributor_id"})})
public class HfpayDistributorTransactionStatistics {

    /** 店铺交易统计统计 */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "店铺交易统计统计")
    private Long id;

    /** company_id */
    @MpField(value = "company_id", columnType = "integer", comment = "company_id")
    private Integer companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "integer", comment = "店铺id")
    private Integer distributorId;

    /** 日期 */
    @MpField(value = "date", columnType = "integer", comment = "日期")
    private Integer date;

    /** 交易总笔数，默认 0 */
    @MpField(value = "order_count", columnType = "integer", comment = "交易总笔数", defaultValue = "0")
    private Integer orderCount = 0;

    /** 总计交易金额，默认 0 */
    @MpField(value = "order_total_fee", columnType = "integer", comment = "总计交易金额", defaultValue = "0")
    private Integer orderTotalFee = 0;

    /** 已退款总笔数，默认 0 */
    @MpField(value = "order_refund_count", columnType = "integer", comment = "已退款总笔数", defaultValue = "0")
    private Integer orderRefundCount = 0;

    /** 退款总金额，默认 0 */
    @MpField(value = "order_refund_total_fee", columnType = "integer", comment = "退款总金额", defaultValue = "0")
    private Integer orderRefundTotalFee = 0;

    /** 在退总笔数，默认 0 */
    @MpField(value = "order_refunding_count", columnType = "integer", comment = "在退总笔数", defaultValue = "0")
    private Integer orderRefundingCount = 0;

    /** 在退总金额，默认 0 */
    @MpField(value = "order_refunding_total_fee", columnType = "integer", comment = "在退总金额", defaultValue = "0")
    private Integer orderRefundingTotalFee = 0;

    /** 已结算手续费，默认 0 */
    @MpField(value = "order_profit_sharing_charge", columnType = "integer", comment = "已结算手续费", defaultValue = "0")
    private Integer orderProfitSharingCharge = 0;

    /** 总手续费，默认 0 */
    @MpField(value = "order_total_charge", columnType = "integer", comment = "总手续费", defaultValue = "0")
    private Integer orderTotalCharge = 0;

    /** 总退款手续费，默认 0 */
    @MpField(value = "order_refund_total_charge", columnType = "integer", comment = "总退款手续费", defaultValue = "0")
    private Integer orderRefundTotalCharge = 0;

    /** 未结算手续费（包含已退款），默认 0 */
    @MpField(value = "order_un_profit_sharing_total_charge", columnType = "integer", comment = "未结算手续费（包含已退款）", defaultValue = "0")
    private Integer orderUnProfitSharingTotalCharge = 0;

    /** 未结算已退款手续费，默认 0 */
    @MpField(value = "order_un_profit_sharing_refund_total_charge", columnType = "integer", comment = "未结算已退款手续费", defaultValue = "0")
    private Integer orderUnProfitSharingRefundTotalCharge = 0;

    /** 创建时间 */
    @MpField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @MpField("updated_at")
    private LocalDateTime updatedAt;
}
