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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 商城关联达达同城配表 */
@Data
@MpTable(value = "company_rel_dada", comment = "商城关联达达同城配表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class CompanyRelDada {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 商户编号 */
    @MpField(value = "source_id", columnType = "string", nullable = true, comment = "商户编号")
    private String sourceId;

    /** 企业全称 */
    @MpField(value = "enterprise_name", columnType = "string", nullable = true, comment = "企业全称")
    private String enterpriseName;

    /** 企业地址 */
    @MpField(value = "enterprise_address", columnType = "string", nullable = true, comment = "企业地址")
    private String enterpriseAddress;

    /** 商户手机号 */
    @MpField(value = "mobile", columnType = "string", nullable = true, comment = "商户手机号")
    private String mobile;

    /** 商户城市名称 */
    @MpField(value = "city_name", columnType = "string", nullable = true, comment = "商户城市名称")
    private String cityName;

    /** 联系人姓名 */
    @MpField(value = "contact_name", columnType = "string", nullable = true, comment = "联系人姓名")
    private String contactName;

    /** 联系人电话 */
    @MpField(value = "contact_phone", columnType = "string", nullable = true, comment = "联系人电话")
    private String contactPhone;

    /** 邮箱地址 */
    @MpField(value = "email", columnType = "string", nullable = true, comment = "邮箱地址")
    private String email;

    /** 运费承担方:0:商家承担，1:买家承担 */
    @MpField(value = "freight_type", columnType = "boolean", comment = "运费承担方:0:商家承担，1:买家承担", defaultValue = "0")
    private Boolean freightType;

    /** 开通状态:0:未开通过，1:已开通过 */
    @MpField(value = "status", columnType = "boolean", comment = "开通状态:0:未开通过，1:已开通过", defaultValue = "0")
    private Boolean status;

    /** 是否开启:0:未开启，1:已开启 */
    @MpField(value = "is_open", columnType = "boolean", comment = "是否开启:0:未开启，1:已开启", defaultValue = "0")
    private Boolean isOpen;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer", nullable = true, comment = "创建时间")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
