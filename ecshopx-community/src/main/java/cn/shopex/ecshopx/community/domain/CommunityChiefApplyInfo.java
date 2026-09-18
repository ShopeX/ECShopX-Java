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

package cn.shopex.ecshopx.community.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 社区拼团团长申请表
 */
@Data
@MpTable(value = "community_chief_apply_info", comment = "社区拼团团长申请表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_chief_mobile", columns = {"chief_mobile"}), @MpIndex(name = "ix_user_id", columns = {"user_id"})})
public class CommunityChiefApplyInfo {

    /** 申请id */
    @MpId(value = "apply_id", type = IdType.AUTO, columnType = "bigint", comment = "申请id")
    private Long applyId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 店铺id，为 0 时表示平台的团长申请 */
    @MpField(value = "distributor_id", columnType = "integer", comment = "店铺id,为0时表示平台的团长申请", defaultValue = "0")
    private Integer distributorId = 0;

    /** 会员ID */
    @MpField(value = "user_id", columnType = "bigint", comment = "会员ID")
    private Long userId;

    /** 团长名称 */
    @MpField(value = "chief_name", columnType = "string", comment = "团长名称")
    private String chiefName;

    /** 团长手机号 */
    @MpField(value = "chief_mobile", columnType = "string", comment = "团长手机号")
    private String chiefMobile;

    /** 附加信息 */
    @MpField(value = "extra_data", columnType = "text", comment = "附加信息")
    private String extraData;

    /**
     * 审批状态：0 未审批；1 同意；2 驳回。默认 0。
     */
    @MpField(value = "approve_status", columnType = "integer", comment = "审批状态 0:未审批 1:同意 2:驳回", defaultValue = "0")
    private Integer approveStatus = 0;

    /** 拒绝原因，可为空 */
    @MpField(value = "refuse_reason", columnType = "text", nullable = true, comment = "拒绝原因")
    private String refuseReason;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created_at", columnType = "integer")
    private Integer createdAt;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated_at", columnType = "integer", nullable = true)
    private Integer updatedAt;
}
