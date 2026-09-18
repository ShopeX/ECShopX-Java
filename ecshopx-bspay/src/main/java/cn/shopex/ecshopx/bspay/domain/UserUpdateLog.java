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

package cn.shopex.ecshopx.bspay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 用户进件修改log(审核成功后修改)
 */
@Data
@MpTable(value = "bspay_user_update_log", comment = "用户进件修改log(审核成功后修改)", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_sys_id", columns = {"sys_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_huifu_id", columns = {"huifu_id"})})
public class UserUpdateLog {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long companyId;

    /** sys_id */
    @MpField(value = "sys_id", columnType = "string", length = 100, comment = "sys_id")
    private String sysId = "";

    /** 汇付ID */
    @MpField(value = "huifu_id", columnType = "string", nullable = true, comment = "汇付ID")
    private String huifuId = "";

    /**
     * 进件类型，默认 indv；indv=个人，ent=企业
     */
    @MpField(value = "user_type", columnType = "string", length = 20, comment = "进件类型", defaultValue = "indv")
    private String userType = "indv";

    /** 用户表的主键id */
    @MpField(value = "user_id", columnType = "string", comment = "用户表的主键id")
    private String userId;

    /** 修改数据 json */
    @MpField(value = "data", columnType = "text", comment = "修改数据 json")
    private String data;

    /**
     * 审核状态，状态包括：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；E-开户和创建结算账户成功
     */
    @MpField(value = "audit_state", columnType = "string", length = 50, nullable = true, comment = "审核状态，状态包括：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；E-开户和创建结算账户成功", defaultValue = "A")
    private String auditState = "A";

    /** 审核结果描述 */
    @MpField(value = "audit_desc", columnType = "string", length = 500, nullable = true, comment = "审核结果描述")
    private String auditDesc = "";

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
