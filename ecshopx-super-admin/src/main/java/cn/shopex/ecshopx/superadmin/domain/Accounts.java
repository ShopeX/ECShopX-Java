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

package cn.shopex.ecshopx.superadmin.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 平台账号表 */
@Data
@MpTable(value = "super_admin_accounts", comment = "平台账号表")
public class Accounts {

    /** 账号id */
    @MpId(value = "account_id", type = IdType.AUTO, columnType = "bigint", comment = "账号id")
    private Long accountId;

    /** 登录账号名 */
    @MpField(value = "login_name", columnType = "string", comment = "登录账号名")
    private String loginName;

    /** 密码 */
    @MpField(value = "password", columnType = "string", comment = "密码")
    private String password;

    /** 姓名 */
    @MpField(value = "name", columnType = "string", comment = "姓名")
    private String name;

    /** 是否超级管理员，默认 false */
    @MpField(value = "super", columnType = "boolean", comment = "是否超级管理员", defaultValue = "False")
    private Boolean superAdmin = false;

    /** 是否启用，默认 false */
    @MpField(value = "status", columnType = "boolean", comment = "是否启用", defaultValue = "False")
    private Boolean status = false;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
