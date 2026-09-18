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

package cn.shopex.ecshopx.espier.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 线下转账收款账户 */
@Data
@MpTable(value = "offline_bank_account", comment = "线下转账收款账户", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_is_default", columns = {"is_default"})})
public class OfflineBankAccount {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 收款账户名称 */
    @MpField(value = "bank_account_name", columnType = "string", length = 50, comment = "收款账户名称")
    private String bankAccountName;

    /** 银行账号 */
    @MpField(value = "bank_account_no", columnType = "string", length = 30, comment = "银行账号")
    private String bankAccountNo;

    /** 开户银行 */
    @MpField(value = "bank_name", columnType = "string", length = 100, comment = "开户银行")
    private String bankName;

    /** 银联号 */
    @MpField(value = "china_ums_no", columnType = "string", length = 20, comment = "银联号")
    private String chinaUmsNo;

    /** 图片 */
    @MpField(value = "pic", columnType = "string", length = 255, comment = "图片")
    private String pic;

    /** 备注 */
    @MpField(value = "remark", columnType = "string", length = 255, comment = "备注")
    private String remark;

    /** 是否默认 */
    @MpField(value = "is_default", columnType = "boolean", comment = "是否默认", defaultValue = "0")
    private Boolean isDefault = false;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
