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

/** 会员标签组与标签关系表 */
@Data
@MpTable(value = "members_tag_group_rel", comment = "会员标签组与标签关系表", indexes = {@MpIndex(name = "idx_group_id", columns = {"group_id"}), @MpIndex(name = "idx_tag_id", columns = {"tag_id"}), @MpIndex(name = "idx_company_distributor", columns = {"company_id", "distributor_id"})})
public class MemberTagGroupRel {

    /** 主键ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "主键ID")
    private Long id;

    /** 标签组ID */
    @MpField(value = "group_id", columnType = "bigint", comment = "标签组ID")
    private Long groupId;

    /** 标签ID */
    @MpField(value = "tag_id", columnType = "bigint", comment = "标签ID")
    private Long tagId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;
}
