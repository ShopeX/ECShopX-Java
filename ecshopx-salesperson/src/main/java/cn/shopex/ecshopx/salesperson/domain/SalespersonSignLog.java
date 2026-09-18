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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 导购到/签退记录 */
@Data
@MpTable(value = "salesperson_signup_log", comment = "导购到/签退记录", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_salesperson_id", columns = {"salesperson_id"})})
public class SalespersonSignLog {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 导购员id */
    @MpField(value = "salesperson_id", columnType = "bigint", comment = "导购员id")
    private Long salespersonId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** signin 签到, signout 主动签退, forceout 被动签退 */
    @MpField(value = "sign_type", columnType = "string", comment = "signin 签到, signout 主动签退, forceout 被动签退")
    private String signType;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
