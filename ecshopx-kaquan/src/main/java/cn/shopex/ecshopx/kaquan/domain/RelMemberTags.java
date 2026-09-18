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

package cn.shopex.ecshopx.kaquan.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 卡券关联用户标签表 */
@Data
@MpTable(value = "kaquan_rel_member_tags", comment = "卡券关联用户标签表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_tag_id", columns = {"tag_id"})}, uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"card_id", "tag_id"})})
public class RelMemberTags {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 卡券id */
    @MpField(value = "card_id", columnType = "bigint", length = 64, comment = "卡券id")
    private Long cardId;

    /** 用户标签id */
    @MpField(value = "tag_id", columnType = "bigint", length = 64, comment = "用户标签id")
    private Long tagId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long companyId;
}
