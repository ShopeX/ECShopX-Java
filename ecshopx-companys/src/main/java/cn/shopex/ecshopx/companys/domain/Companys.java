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

import java.time.LocalDateTime;

/**
 * 公司表
 */
@Data
@MpTable(value = "companys", comment = "公司表", indexes = {@MpIndex(name = "idx_pc_domain", columns = {"pc_domain"}), @MpIndex(name = "idx_h5_domain", columns = {"h5_domain"})}, uniqueIndexes = {@MpIndex(name = "idx_passportuid", columns = {"passport_uid"})})
public class Companys {

    /** 公司id */
    @MpId(value = "company_id", type = IdType.AUTO, columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 公司名称 */
    @MpField(value = "company_name", columnType = "string", length = 255, nullable = true, comment = "公司名称")
    private String companyName;

    /** PC域名 */
    @MpField(value = "pc_domain", columnType = "string", length = 200, nullable = true, comment = "PC域名")
    private String pcDomain;

    /** H5域名 */
    @MpField(value = "h5_domain", columnType = "string", length = 200, nullable = true, comment = "H5域名")
    private String h5Domain;

    @MpField(value = "eid", columnType = "string", nullable = true)
    private String eid;

    @MpField(value = "passport_uid", columnType = "string", nullable = true)
    private String passportUid;

    /** 公司管理员id */
    @MpField(value = "company_admin_operator_id", columnType = "bigint", comment = "公司管理员id")
    private Long companyAdminOperatorId;

    /** 所属行业 */
    @MpField(value = "industry", columnType = "string", nullable = true, comment = "所属行业")
    private String industry;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 过期时间 */
    @MpField(value = "expiredAt", columnType = "bigint", nullable = true, comment = "过期时间")
    private Long expiredAt;

    /** 是否禁用 */
    @MpField(value = "is_disabled", columnType = "boolean", comment = "是否禁用", defaultValue = "0")
    private Boolean isDisabled = false;

    /** 第三方特殊字段存储 */
    @MpField(value = "third_params", columnType = "json_array", nullable = true, comment = "第三方特殊字段存储")
    private String thirdParams;

    /** 导购员数量 */
    @MpField(value = "salesman_limit", columnType = "integer", comment = "导购员数量", defaultValue = "20")
    private Integer salesmanLimit = 20;

    /** 是否开启pc模板 1 开启 2不开启 */
    @MpField(value = "is_open_pc_template", columnType = "integer", nullable = true, comment = "是否开启pc模板 1 开启 2不开启", defaultValue = "1")
    private Integer isOpenPcTemplate = 1;

    /** 是否开启域名配置 1 开启 2不开启 */
    @MpField(value = "is_open_domain_setting", columnType = "integer", nullable = true, comment = "是否开启域名配置 1 开启 2不开启", defaultValue = "2")
    private Integer isOpenDomainSetting = 2;

    /** 菜单类型。2:'b2c',3:'platform',4:'standard',5:'in_purchase' */
    @MpField(value = "menu_type", columnType = "integer", comment = "菜单类型。2:'b2c',3:'platform',4:'standard',5:'in_purchase'", defaultValue = "3")
    private Integer menuType = 3;

    @MpField(value = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}
