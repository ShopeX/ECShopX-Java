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

package cn.shopex.ecshopx.wsugc.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 角标 */
@Data
@MpTable(value = "wsugc_badge", comment = "角标", indexes = {@MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_mobile", columns = {"mobile"})})
public class Badge {

    @MpId(value = "badge_id", type = IdType.AUTO, columnType = "bigint")
    private Long badgeId;

    /** 角标名称 */
    @MpField(value = "badge_name", columnType = "string", length = 250, comment = "角标名称")
    private String badgeName;

    /** 角标备注 */
    @MpField(value = "badge_memo", columnType = "string", length = 250, nullable = true, comment = "角标备注")
    private String badgeMemo;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", length = 50, nullable = true, comment = "手机号")
    private String mobile;

    /** 排序 */
    @MpField(value = "p_order", columnType = "integer", comment = "排序", defaultValue = "0")
    private Integer pOrder = 0;

    /** 添加时间 */
    @MpField(value = "created", columnType = "integer", comment = "添加时间")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 机器审核时间 */
    @MpField(value = "ai_verify_time", columnType = "bigint", nullable = true, comment = "机器审核时间", defaultValue = "0")
    private Long aiVerifyTime = 0L;

    /** 人工审核时间 */
    @MpField(value = "manual_verify_time", columnType = "bigint", nullable = true, comment = "人工审核时间", defaultValue = "0")
    private Long manualVerifyTime = 0L;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 是否启用. */
    @MpField(value = "enabled", columnType = "integer", comment = "是否启用.", defaultValue = "1")
    private Integer enabled = 1;

    /** 审核状态(0待审核,1审核通过,2机器拒绝,3待人工审核,4人工拒绝) */
    @MpField(value = "status", columnType = "integer", comment = "审核状态(0待审核,1审核通过,2机器拒绝,3待人工审核,4人工拒绝)", defaultValue = "0")
    private Integer status = 0;

    /** 机器拒绝理由 */
    @MpField(value = "ai_refuse_reason", columnType = "string", nullable = true, comment = "机器拒绝理由")
    private String aiRefuseReason;

    /** 人工拒绝理由 */
    @MpField(value = "manual_refuse_reason", columnType = "string", nullable = true, comment = "人工拒绝理由")
    private String manualRefuseReason;

    /** 是否置顶. */
    @MpField(value = "is_top", columnType = "integer", comment = "是否置顶.", defaultValue = "0")
    private Integer isTop = 0;

    /** 创建的用户 */
    @MpField(value = "user_id", columnType = "bigint", comment = "创建的用户", defaultValue = "0")
    private Long userId = 0L;

    /** 来源 1用户,2官方 */
    @MpField(value = "source", columnType = "integer", nullable = true, comment = "来源 1用户,2官方", defaultValue = "1")
    private Integer source = 1;

    /** 管理员id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "管理员id", defaultValue = "0")
    private Long operatorId = 0L;
}
