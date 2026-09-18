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

/** 企业员工表 */
@Data
@MpTable(value = "employee_purchase_employees", comment = "企业员工表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "idx_mobile", columns = {"mobile"}, lengths = {64}), @MpIndex(name = "idx_enterprise_userid", columns = {"enterprise_id", "user_id"})})
public class Employees {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 店铺id,为0时表示为商城的员工 */
    @MpField(value = "distributor_id", columnType = "integer", comment = "店铺id,为0时表示为商城的员工", defaultValue = "0")
    private Integer distributorId = 0;

    /** 操作id */
    @MpField(value = "operator_id", columnType = "integer", comment = "操作id", defaultValue = "0")
    private Integer operatorId = 0;

    /** 姓名 */
    @MpField(value = "name", columnType = "string", length = 500, comment = "姓名")
    private String name;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", length = 255, nullable = true, comment = "手机号")
    private String mobile;

    /** 登录账号 */
    @MpField(value = "account", columnType = "string", length = 500, nullable = true, comment = "登录账号")
    private String account;

    /** 邮箱 */
    @MpField(value = "email", columnType = "string", length = 500, nullable = true, comment = "邮箱")
    private String email;

    /** 校验码 */
    @MpField(value = "auth_code", columnType = "string", length = 50, nullable = true, comment = "校验码")
    private String authCode;

    /** 企业id */
    @MpField(value = "enterprise_id", columnType = "bigint", comment = "企业id")
    private Long enterpriseId;

    /** 会员id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "会员id")
    private Long userId;

    /** 会员手机号 */
    @MpField(value = "member_mobile", columnType = "string", length = 255, nullable = true, comment = "会员手机号")
    private String memberMobile;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Integer updated;

    /** 失效 */
    @MpField(value = "disabled", columnType = "boolean", comment = "失效", defaultValue = "0")
    private Boolean disabled = false;
}
