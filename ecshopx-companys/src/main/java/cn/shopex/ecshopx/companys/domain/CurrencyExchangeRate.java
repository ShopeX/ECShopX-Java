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

package cn.shopex.ecshopx.companys.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 货币汇率
 */
@Data
@MpTable(value = "companys_currency_exchange_rate", comment = "货币汇率")
public class CurrencyExchangeRate {

    /** 公司id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "公司id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 货币描述 */
    @MpField(value = "title", columnType = "string", nullable = true, comment = "货币描述")
    private String title;

    /** 货币英文缩写 */
    @MpField(value = "currency", columnType = "string", nullable = true, comment = "货币英文缩写")
    private String currency;

    /** 货币符号 */
    @MpField(value = "symbol", columnType = "string", comment = "货币符号")
    private String symbol;

    /** 货币汇率(与人民币) */
    @MpField(value = "rate", columnType = "float", precision = 15, scale = 4, comment = "货币汇率(与人民币)")
    private Double rate;

    /** 是否默认货币 */
    @MpField(value = "is_default", columnType = "boolean", comment = "是否默认货币", defaultValue = "0")
    private Boolean isDefault = false;

    /** 适用端。可选值为 service,normal */
    @MpField(value = "use_platform", columnType = "string", comment = "适用端。可选值为 service,normal", defaultValue = "normal")
    private String usePlatform = "normal";
}
