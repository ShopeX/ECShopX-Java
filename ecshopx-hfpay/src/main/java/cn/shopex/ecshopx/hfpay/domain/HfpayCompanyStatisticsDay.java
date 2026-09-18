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
 * 平台分账数据统计
 */
@Data
@MpTable(value = "hfpay_company_statistics_day", comment = "平台分账数据统计", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class HfpayCompanyStatisticsDay {

    /** 汇付平台分账数据统计 */
    @MpId(value = "hfpay_company_day_id", type = IdType.AUTO, columnType = "bigint", comment = "汇付平台分账数据统计")
    private Long hfpayCompanyDayId;

    /** company_id */
    @MpField(value = "company_id", columnType = "integer", comment = "company_id")
    private Integer companyId;

    /**
     * type 1全部数据 2平台数据 3非平台数据。1 所有数据；2 平台数据；3 非平台数据
     */
    @MpField(value = "type", columnType = "integer", comment = "type 1全部数据 2平台数据 3非平台数据")
    private Integer type;

    /** 日期 */
    @MpField(value = "date", columnType = "integer", comment = "日期")
    private Integer date;

    /** 总计收入，可为空 */
    @MpField(value = "income", columnType = "integer", nullable = true, comment = "总计收入")
    private Integer income;

    /** 总计支出，可为空 */
    @MpField(value = "disburse", columnType = "integer", nullable = true, comment = "总计支出")
    private Integer disburse;

    /** 总计提现，可为空 */
    @MpField(value = "withdrawal", columnType = "integer", nullable = true, comment = "总计提现")
    private Integer withdrawal;

    /** 余额，可为空 */
    @MpField(value = "balance", columnType = "integer", nullable = true, comment = "余额")
    private Integer balance;

    /** 可提现余额，可为空 */
    @MpField(value = "withdrawal_balance", columnType = "integer", nullable = true, comment = "可提现余额")
    private Integer withdrawalBalance;

    /** 未结算资金，可为空 */
    @MpField(value = "unsettled_funds", columnType = "integer", nullable = true, comment = "未结算资金")
    private Integer unsettledFunds;

    /** 合计退款，可为空 */
    @MpField(value = "refund", columnType = "integer", nullable = true, comment = "合计退款")
    private Integer refund;

    /** 已结算资金，可为空 */
    @MpField(value = "settlement_funds", columnType = "integer", nullable = true, comment = "已结算资金")
    private Integer settlementFunds;

    /** 创建时间 */
    @MpField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @MpField("updated_at")
    private LocalDateTime updatedAt;
}
