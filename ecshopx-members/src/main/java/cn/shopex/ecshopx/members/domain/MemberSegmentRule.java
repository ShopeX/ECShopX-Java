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

package cn.shopex.ecshopx.members.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 人群规则表 */
@Data
@MpTable(value = "member_segment_rules", comment = "人群规则表", indexes = {@MpIndex(name = "idx_company_distributor", columns = {"company_id", "distributor_id"}), @MpIndex(name = "idx_status", columns = {"status"}), @MpIndex(name = "idx_created", columns = {"created"})})
public class MemberSegmentRule {

    /** 规则id */
    @MpId(value = "rule_id", type = IdType.AUTO, columnType = "bigint", comment = "规则id")
    private Long ruleId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 规则名称（分群标签名称） */
    @MpField(value = "rule_name", columnType = "string", length = 100, comment = "规则名称（分群标签名称）")
    private String ruleName;

    /** 人群说明 */
    @MpField(value = "description", columnType = "text", nullable = true, comment = "人群说明")
    private String description;

    /** 规则配置（层级结构，JSON格式存储） */
    @MpField(value = "rule_config", columnType = "text", comment = "规则配置（层级结构，JSON格式存储）")
    private String ruleConfig;

    /** 关联的标签ID数组（JSON格式） */
    @MpField(value = "tag_ids", columnType = "text", nullable = true, comment = "关联的标签ID数组（JSON格式）")
    private String tagIds;

    /** 状态：0=禁用，1=启用 */
    @MpField(value = "status", columnType = "smallint", comment = "状态：0=禁用，1=启用", defaultValue = "1")
    private Integer status = 1;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", nullable = true, columnDefinition = "bigint NOT NULL")
    private Long updated;
}
