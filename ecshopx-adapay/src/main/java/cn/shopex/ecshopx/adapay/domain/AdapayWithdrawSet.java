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

package cn.shopex.ecshopx.adapay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 提现设置表
 */
@Data
@MpTable(value = "adapay_withdraw_set", comment = "提现设置表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class AdapayWithdrawSet {

    /** 提现配置表id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "提现配置表id")
    private Long id;

    /** 是否支持自动提现 */
    @MpField(value = "isAuto", columnType = "boolean", comment = "是否支持自动提现")
    private Boolean isAuto;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 经销商 id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "经销商 id")
    private Long distributorId;

    /** 取现金额，必须大于0，人民币为元 */
    @MpField(value = "cash_amt", columnType = "string", nullable = true, comment = "取现金额，必须大于0，人民币为元")
    private String cashAmt;

    /** 取现方式 T0：T0取现; T1：T1取现 D1：D1取现 */
    @MpField(value = "cash_type", columnType = "string", comment = "取现方式 T0：T0取现; T1：T1取现 D1：D1取现")
    private String cashType;

    /** 提现规则（库中为数组序列化形式，业务层按既有格式解析） */
    @MpField(value = "rule", columnType = "array", comment = "提现规则")
    private String rule;
}
