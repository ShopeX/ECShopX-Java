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

/** 企业发件邮箱 */
@Data
@MpTable(value = "employee_purchase_enterprise_email_box", comment = "企业发件邮箱", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_enterprise_id", columns = {"enterprise_id"})})
public class EnterpriseEmailBox {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 企业白名单id */
    @MpField(value = "enterprise_id", columnType = "bigint", comment = "企业白名单id")
    private Long enterpriseId;

    /** 端口号 */
    @MpField(value = "smtp_port", columnType = "string", length = 10, comment = "端口号")
    private String smtpPort;

    /** 服务器主机地址 */
    @MpField(value = "relay_host", columnType = "string", length = 50, comment = "服务器主机地址")
    private String relayHost;

    /** 服务器用户名 */
    @MpField(value = "user", columnType = "string", length = 50, comment = "服务器用户名")
    private String user;

    /** 服务器密码 */
    @MpField(value = "password", columnType = "string", length = 50, comment = "服务器密码")
    private String password;

    /** 员工收件邮箱后缀 */
    @MpField(value = "suffix", columnType = "string", length = 50, comment = "员工收件邮箱后缀")
    private String suffix;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
