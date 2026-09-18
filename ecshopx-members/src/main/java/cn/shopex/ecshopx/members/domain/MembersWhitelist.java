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

/** 会员白名单 */
@Data
@MpTable(value = "members_whitelist", comment = "会员白名单", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_mobile", columns = {"mobile"}, lengths = {64})}, uniqueIndexes = {@MpIndex(name = "mobile_company", columns = {"mobile", "company_id"})})
public class MembersWhitelist {

    /** 白名单id */
    @MpId(value = "whitelist_id", type = IdType.AUTO, columnType = "bigint", comment = "白名单id")
    private Long whitelistId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", length = 255, comment = "手机号")
    private String mobile;

    /** 名称 */
    @MpField(value = "name", columnType = "string", length = 500, comment = "名称")
    private String name;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
