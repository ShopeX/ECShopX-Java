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
 * 银行代码
 */
@Data
@MpTable(value = "adapay_bank_codes", comment = "银行代码", indexes = {@MpIndex(name = "idx_bank_name", columns = {"bank_name"}), @MpIndex(name = "idx_bank_code", columns = {"bank_code"})})
public class AdapayBankCodes {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 银行名称 */
    @MpField(value = "bank_name", columnType = "string", length = 100, comment = "银行名称")
    private String bankName;

    /** 银行代码 */
    @MpField(value = "bank_code", columnType = "string", length = 50, comment = "银行代码")
    private String bankCode;
}
