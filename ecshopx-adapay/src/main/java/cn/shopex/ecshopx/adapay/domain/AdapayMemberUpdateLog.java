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

package cn.shopex.ecshopx.adapay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * adapay子账户修改log(审核成功后修改)
 */
@Data
@MpTable(value = "adapay_member_update_log", comment = "adapay子账户修改log(审核成功后修改)", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_app_id", columns = {"app_id"}), @MpIndex(name = "idx_member_id", columns = {"member_id"})})
public class AdapayMemberUpdateLog {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "string", comment = "公司id")
    private String companyId;

    /** 应用app_id */
    @MpField(value = "app_id", columnType = "string", length = 100, comment = "应用app_id")
    private String appId = "";

    /** adapay_member的主键id(汇付id) */
    @MpField(value = "member_id", columnType = "string", comment = "adapay_member的主键id(汇付id)")
    private String memberId;

    /** 修改数据 json */
    @MpField(value = "data", columnType = "text", comment = "修改数据 json")
    private String data;

    /** 审核状态，状态包括：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；E-开户和创建结算账户成功 */
    @MpField(value = "audit_state", columnType = "string", length = 50, nullable = true, comment = "审核状态，状态包括：A-待审核；B-审核失败；C-开户失败；D-开户成功但未创建结算账户；E-开户和创建结算账户成功")
    private String auditState = "";

    /** 审核结果描述 */
    @MpField(value = "audit_desc", columnType = "string", length = 500, nullable = true, comment = "审核结果描述")
    private String auditDesc = "";

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
