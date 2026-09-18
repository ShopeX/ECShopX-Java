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

package cn.shopex.ecshopx.popularize.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 推广员身份表 */
@Data
@MpTable(value = "popularize_promoter_identity", comment = "推广员身份表", indexes = {@MpIndex(name = "idx_companyid", columns = {"company_id"}), @MpIndex(name = "idx_is_subordinates", columns = {"is_subordinates"})})
public class PromoterIdentity {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 企业ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "企业ID")
    private Long companyId;

    /** 推广员身份名称 */
    @MpField(value = "name", columnType = "string", nullable = true, comment = "推广员身份名称")
    private String name;

    /** 是否可发展下级分销员 */
    @MpField(value = "is_subordinates", columnType = "integer", length = 4, comment = "是否可发展下级分销员")
    private Integer isSubordinates;

    /** 是否为默认 */
    @MpField(value = "is_default", columnType = "integer", length = 4, comment = "是否为默认", defaultValue = "0\"")
    private Integer isDefault = 0;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
