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

package cn.shopex.ecshopx.salesperson.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 分润记录表 */
@Data
@MpTable(value = "companys_profit", comment = "分润记录表")
public class Profit {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "string", length = 64, nullable = true, comment = "订单号")
    private String orderId;

    /** 分润类型id */
    @MpField(value = "profit_user_id", columnType = "bigint", comment = "分润类型id")
    private Long profitUserId;

    /** 1 用户 2 店铺 3 区域经销商 4 总部 */
    @MpField(value = "profit_user_type", columnType = "bigint", comment = "1 用户 2 店铺 3 区域经销商 4 总部")
    private Long profitUserType;

    /** 分润金额，以分为单位 */
    @MpField(value = "total_fee", columnType = "bigint", nullable = true, comment = "分润金额，以分为单位")
    private Long totalFee;

    /** 冻结金额，以分为单位 */
    @MpField(value = "frozen_fee", columnType = "bigint", nullable = true, comment = "冻结金额，以分为单位")
    private Long frozenFee;

    /** 可以提现金额，以分为单位 */
    @MpField(value = "withdrawals_fee", columnType = "bigint", nullable = true, comment = "可以提现金额，以分为单位")
    private Long withdrawalsFee;

    /** 已提现金额，以分为单位 */
    @MpField(value = "cashed_fee", columnType = "bigint", nullable = true, comment = "已提现金额，以分为单位")
    private Long cashedFee;

    /** 拉新提成（导购｜门店） */
    @MpField(value = "commissions", columnType = "bigint", nullable = true, comment = "拉新提成（导购｜门店）")
    private Long commissions;

    /** 推广提成（导购） */
    @MpField(value = "popularize_commissions", columnType = "bigint", nullable = true, comment = "推广提成（导购）")
    private Long popularizeCommissions;

    /** 货款（发货门店｜总部） */
    @MpField(value = "goods_amount", columnType = "bigint", nullable = true, comment = "货款（发货门店｜总部）")
    private Long goodsAmount;

    /** 补贴（经销商） */
    @MpField(value = "subsidy", columnType = "bigint", nullable = true, comment = "补贴（经销商）")
    private Long subsidy;
}
