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
@MpTable(value = "companys_profit_log", comment = "分润记录表")
public class ProfitLog {

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

    /** 1 导购 2 店铺 3 区域经销商 4 总部 */
    @MpField(value = "profit_user_type", columnType = "bigint", comment = "1 导购 2 店铺 3 区域经销商 4 总部")
    private Long profitUserType;

    /** 1 拉新分润 2 推广提成 3 货款 4 补贴 */
    @MpField(value = "profit_type", columnType = "bigint", comment = "1 拉新分润 2 推广提成 3 货款 4 补贴")
    private Long profitType;

    /** 资金状态 1 取消退款 2 售后退款 3 提现扣减 11 购物冻结 12 购物可体现 */
    @MpField(value = "status", columnType = "bigint", comment = "资金状态 1 取消退款 2 售后退款 3 提现扣减 11 购物冻结 12 购物可体现")
    private Long status;

    /** 增加金额 */
    @MpField(value = "income_fee", columnType = "bigint", nullable = true, comment = "增加金额")
    private Long incomeFee;

    /** 扣减金额 */
    @MpField(value = "outcome_fee", columnType = "bigint", nullable = true, comment = "扣减金额")
    private Long outcomeFee;

    /** 资金状态 */
    @MpField(value = "remark", columnType = "string", length = 60, comment = "资金状态")
    private String remark;

    /** 冗余参数 例如 order_id */
    @MpField(value = "params", columnType = "json_array", nullable = true, comment = "冗余参数 例如 order_id")
    private String params;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
