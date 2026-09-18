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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 门店人员 */
@Data
@MpTable(value = "shop_salesperson", comment = "门店人员")
public class ShopSalesperson {

    /** 门店人员ID */
    @MpId(value = "salesperson_id", type = IdType.AUTO, columnType = "bigint", comment = "门店人员ID")
    private Long salespersonId;

    /** 姓名 */
    @MpField(value = "name", columnType = "string", length = 500, comment = "姓名")
    private String name;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", comment = "手机号")
    private String mobile;

    /** 创建时间 */
    @MpField(value = "created_time", columnType = "string", comment = "创建时间")
    private String createdTime;

    /** 人员类型 admin: 管理员; verification_clerk:核销员; shopping_guide:导购员 */
    @MpField(value = "salesperson_type", columnType = "string", comment = "人员类型 admin: 管理员; verification_clerk:核销员; shopping_guide:导购员", defaultValue = "admin")
    private String salespersonType = "admin";

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 关联会员id */
    @MpField(value = "user_id", columnType = "integer", comment = "关联会员id", defaultValue = "0")
    private Integer userId = 0;

    /** 导购员引入的会员数 */
    @MpField(value = "child_count", columnType = "integer", comment = "导购员引入的会员数", defaultValue = "0")
    private Integer childCount = 0;

    /** 是否有效 */
    @MpField(value = "is_valid", columnType = "string", comment = "是否有效", defaultValue = "true")
    private String isValid = "true";

    /** 门店id */
    @MpField(value = "shop_id", columnType = "string", nullable = true, comment = "门店id")
    private String shopId = "0";

    /** 门店名称 */
    @MpField(value = "shop_name", columnType = "string", nullable = true, comment = "门店名称")
    private String shopName;

    /** 导购员编号 */
    @MpField(value = "number", columnType = "string", length = 50, nullable = true, comment = "导购员编号")
    private String number;

    /** 导购员会员好友数 */
    @MpField(value = "friend_count", columnType = "integer", comment = "导购员会员好友数", defaultValue = "0")
    private Integer friendCount = 0;

    /** 企业微信userid[如果是内部应用则是明文，如果是第三方应用则是密文] */
    @MpField(value = "work_userid", columnType = "string", nullable = true, comment = "企业微信userid[如果是内部应用则是明文，如果是第三方应用则是密文]")
    private String workUserid;

    /** 企业微信userid[用于对接导购存储明文userid] */
    @MpField(value = "work_clear_userid", columnType = "string", nullable = true, comment = "企业微信userid[用于对接导购存储明文userid]")
    private String workClearUserid;

    /** 企业微信头像 */
    @MpField(value = "avatar", columnType = "string", nullable = true, comment = "企业微信头像")
    private String avatar;

    /** 企业微信userid */
    @MpField(value = "work_configid", columnType = "string", nullable = true, comment = "企业微信userid")
    private String workConfigid;

    /** 企业微信userid */
    @MpField(value = "work_qrcode_configid", columnType = "string", nullable = true, comment = "企业微信userid")
    private String workQrcodeConfigid;

    /** 导购权限集合 */
    @MpField(value = "role", columnType = "string", nullable = true, comment = "导购权限集合")
    private String role;

    /** 职务 */
    @MpField(value = "salesperson_job", columnType = "string", length = 50, nullable = true, comment = "职务")
    private String salespersonJob = "";

    /** 员工类型 [1 员工] [2 编外] */
    @MpField(value = "employee_status", columnType = "integer", comment = "员工类型 [1 员工] [2 编外]", defaultValue = "1")
    private Integer employeeStatus = 1;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
