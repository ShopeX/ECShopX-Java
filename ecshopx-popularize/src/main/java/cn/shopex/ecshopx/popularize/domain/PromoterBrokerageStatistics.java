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

package cn.shopex.ecshopx.popularize.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 分销佣金统计 */
@Data
@MpTable(value = "popularize_brokerage_statistics", comment = "分销佣金统计", indexes = {@MpIndex(name = "ix_user_id", columns = {"user_id"}), @MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class PromoterBrokerageStatistics {

    /** 用户id */
    @MpId(value = "user_id", type = IdType.INPUT, columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 分销商品总金额 */
    @MpField(value = "item_total_price", columnType = "bigint", nullable = true, comment = "分销商品总金额", defaultValue = "0")
    private Long itemTotalPrice = 0L;

    /** 分销佣金总金额 */
    @MpField(value = "rebate_total", columnType = "bigint", nullable = true, comment = "分销佣金总金额", defaultValue = "0")
    private Long rebateTotal = 0L;

    /** 分销佣金总积分 */
    @MpField(value = "point_total", columnType = "bigint", comment = "分销佣金总积分", defaultValue = "0")
    private Long pointTotal = 0L;

    /** 未结算佣金 */
    @MpField(value = "no_close_rebate", columnType = "bigint", nullable = true, comment = "未结算佣金", defaultValue = "0")
    private Long noCloseRebate = 0L;

    /** 未结算佣金积分 */
    @MpField(value = "no_close_point", columnType = "bigint", comment = "未结算佣金积分", defaultValue = "0")
    private Long noClosePoint = 0L;

    /** 可提现佣金 */
    @MpField(value = "cash_withdrawal_rebate", columnType = "bigint", nullable = true, comment = "可提现佣金", defaultValue = "0")
    private Long cashWithdrawalRebate = 0L;

    /** 订单返佣中可使用积分 */
    @MpField(value = "cash_withdrawal_point", columnType = "bigint", comment = "订单返佣中可使用积分", defaultValue = "0")
    private Long cashWithdrawalPoint = 0L;

    /** 申请提现佣金，冻结提现佣金 */
    @MpField(value = "freeze_cash_withdrawal_rebate", columnType = "bigint", nullable = true, comment = "申请提现佣金，冻结提现佣金", defaultValue = "0")
    private Long freezeCashWithdrawalRebate = 0L;

    /** 已提现佣金 */
    @MpField(value = "payed_rebate", columnType = "bigint", nullable = true, comment = "已提现佣金", defaultValue = "0")
    private Long payedRebate = 0L;

    /** 充值返佣 */
    @MpField(value = "recharge_rebate", columnType = "bigint", nullable = true, comment = "充值返佣", defaultValue = "0")
    private Long rechargeRebate = 0L;

    /** 充值返佣金积分 */
    @MpField(value = "recharge_point", columnType = "bigint", comment = "充值返佣金积分", defaultValue = "0")
    private Long rechargePoint = 0L;
}
