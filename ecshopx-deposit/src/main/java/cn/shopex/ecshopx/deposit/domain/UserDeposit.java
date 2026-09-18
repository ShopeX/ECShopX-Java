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

package cn.shopex.ecshopx.deposit.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 用户储值金额表 */
@Data
@MpTable(value = "user_deposit", comment = "用户储值金额表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"})})
public class UserDeposit {

    /** 用户储值ID */
    @MpId(value = "deposit_id", type = IdType.AUTO, columnType = "bigint", comment = "用户储值ID")
    private Long depositId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 充值金额/消费金额。 单位是分 */
    @MpField(value = "money", columnType = "bigint", comment = "充值金额/消费金额。 单位是分")
    private Long money;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
