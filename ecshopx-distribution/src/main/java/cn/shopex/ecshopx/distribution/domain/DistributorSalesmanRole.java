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

/** 店铺权限表 */
@Data
@MpTable(value = "distribution_distributor_salesman_role", comment = "店铺权限表")
public class DistributorSalesmanRole {

    @MpId(value = "salesman_role_id", type = IdType.AUTO, columnType = "bigint")
    private Long salesmanRoleId;

    /** 企业ID */
    @MpField(value = "company_id", columnType = "integer", comment = "企业ID")
    private Integer companyId;

    /** 导购员角色名称 */
    @MpField(value = "role_name", columnType = "string", length = 32, comment = "导购员角色名称")
    private String roleName;

    /** 导购员角色类型 */
    @MpField(value = "rule_ids", columnType = "json_array", nullable = true, comment = "导购员角色类型")
    private String ruleIds;
}
