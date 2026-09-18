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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 员工角色表
 */
@Data
@MpTable(value = "companys_roles", comment = "员工角色表", indexes = {@MpIndex(name = "idx_role_id", columns = {"role_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class Roles {

    /** 角色id */
    @MpId(value = "role_id", type = IdType.AUTO, columnType = "bigint", comment = "角色id")
    private Long roleId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id。为0则代表是平台添加的店铺角色 */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id。为0则代表是平台添加的店铺角色", defaultValue = "0")
    private Long distributorId = 0L;

    /** 角色名称 */
    @MpField(value = "role_name", columnType = "string", length = 32, comment = "角色名称")
    private String roleName;

    /** 角色平台来源,platform: 平台角色，distributor:店铺管理角色 */
    @MpField(value = "role_source", columnType = "string", length = 32, comment = "角色平台来源,platform: 平台角色，distributor:店铺管理角色", defaultValue = "platform")
    private String roleSource = "platform";

    /** 权限,json数据 */
    @MpField(value = "permission", columnType = "text", comment = "权限,json数据")
    private String permission;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
