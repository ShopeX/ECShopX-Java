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

/** 结算周期设置 */
@Data
@MpTable(value = "statement_period_setting", comment = "结算周期设置", indexes = {@MpIndex(name = "idx_supplier_id", columns = {"supplier_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"})})
public class StatementPeriodSetting {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id")
    private Long merchantId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id")
    private Long distributorId;

    /** 供应商ID */
    @MpField(value = "supplier_id", columnType = "bigint", comment = "供应商ID")
    private Long supplierId;

    /** 结算周期：day 天，week 周，month 月 */
    @MpField(value = "period", columnType = "string", comment = "结算周期 day:天 week:周 month:月")
    private String period;

    /** 商户类型：distributor 经销商，supplier 供应商 */
    @MpField(value = "merchant_type", columnType = "string", length = 30, nullable = true, comment = "商户类型：distributor 经销商,supplier 供应商", defaultValue = "distributor")
    private String merchantType;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
