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
@MpTable(value = "companys_profit_statistics", comment = "分润记录表")
public class ProfitStatistics {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 分润月份 */
    @MpField(value = "date", columnType = "string", length = 20, comment = "分润月份")
    private String date;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 分润类型id */
    @MpField(value = "profit_user_id", columnType = "bigint", comment = "分润类型id")
    private Long profitUserId;

    /** 1 用户 2 店铺 3 区域经销商 4 总部 */
    @MpField(value = "profit_user_type", columnType = "bigint", comment = "1 用户 2 店铺 3 区域经销商 4 总部")
    private Long profitUserType;

    /** 提现金额，以分为单位 */
    @MpField(value = "withdrawals_fee", columnType = "bigint", nullable = true, comment = "提现金额，以分为单位", defaultValue = "0")
    private Long withdrawalsFee = 0L;

    /** 提现对象名称 */
    @MpField(value = "name", columnType = "string", length = 100, nullable = true, comment = "提现对象名称")
    private String name = "";

    /** 提现对象名称 */
    @MpField(value = "params", columnType = "json_array", nullable = true, comment = "提现对象名称")
    private String params;
}
