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

package cn.shopex.ecshopx.distribution.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 分销商基础配置 */
@Data
@MpTable(value = "distribution_basic_config", comment = "分销商基础配置")
public class BasicConfig {

    @MpId(value = "company_id", type = IdType.INPUT, columnType = "bigint")
    private Long companyId;

    /** 是否可购买 */
    @MpField(value = "is_buy", columnType = "boolean", comment = "是否可购买")
    private Boolean isBuy;

    /** 提现佣金限制，最少多少金额可提现 */
    @MpField(value = "limit_rebate", columnType = "string", comment = "提现佣金限制，最少多少金额可提现")
    private String limitRebate;

    /** 订单完成后多少天可提现 */
    @MpField(value = "limit_time", columnType = "string", comment = "订单完成后多少天可提现")
    private String limitTime;

    /** 退换货接收人 */
    @MpField(value = "return_name", columnType = "string", comment = "退换货接收人")
    private String returnName;

    /** 退换货地址 */
    @MpField(value = "return_address", columnType = "string", comment = "退换货地址")
    private String returnAddress;

    /** 退换货联系方式 */
    @MpField(value = "return_phone", columnType = "string", comment = "退换货联系方式")
    private String returnPhone;

    /** 是否开启劳务所得税 */
    @MpField(value = "is_income_tax", columnType = "string", comment = "是否开启劳务所得税")
    private String isIncomeTax;

    /** 劳务所得税计算参数 */
    @MpField(value = "income_tax_params", columnType = "string", nullable = true, comment = "劳务所得税计算参数")
    private String incomeTaxParams;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
