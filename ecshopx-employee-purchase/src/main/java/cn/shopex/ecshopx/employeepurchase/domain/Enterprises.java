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

package cn.shopex.ecshopx.employeepurchase.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 企业表 */
@Data
@MpTable(value = "employee_purchase_enterprises", comment = "企业表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "idx_enterprise_sn", columns = {"enterprise_sn"})})
public class Enterprises {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id,为0时表示商城的企业 */
    @MpField(value = "distributor_id", columnType = "integer", comment = "店铺id,为0时表示商城的企业", defaultValue = "0")
    private Integer distributorId = 0;

    /** 操作id */
    @MpField(value = "operator_id", columnType = "integer", comment = "操作id", defaultValue = "0")
    private Integer operatorId = 0;

    /** 企业名称 */
    @MpField(value = "name", columnType = "string", comment = "企业名称")
    private String name;

    /** 企业编码 */
    @MpField(value = "enterprise_sn", columnType = "string", length = 50, comment = "企业编码")
    private String enterpriseSn;

    /** 企业logo */
    @MpField(value = "logo", columnType = "text", nullable = true, comment = "企业logo")
    private String logo;

    /** 二维码背景图 */
    @MpField(value = "qr_code_bg_image", columnType = "text", nullable = true, comment = "二维码背景图")
    private String qrCodeBgImage;

    /** 是否验证员工白名单 */
    @MpField(value = "is_employee_check_enabled", columnType = "boolean", comment = "是否验证员工白名单", defaultValue = "False")
    private Boolean isEmployeeCheckEnabled = false;

    /** 登录类型,mobile:手机号,account:账号,email:邮箱,qr_code:二维码 */
    @MpField(value = "auth_type", columnType = "string", length = 20, comment = "登录类型,mobile:手机号,account:账号,email:邮箱,qr_code:二维码")
    private String authType = "mobile";

    /** 禁用 */
    @MpField(value = "disabled", columnType = "boolean", comment = "禁用", defaultValue = "0")
    private Boolean disabled = false;

    /** 排序 */
    @MpField(value = "sort", columnType = "integer", comment = "排序", defaultValue = "0")
    private Integer sort = 0;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
