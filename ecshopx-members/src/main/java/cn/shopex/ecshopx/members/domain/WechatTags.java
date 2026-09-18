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

/** 微信粉丝标签表 */
@Data
@MpTable(value = "wechat_tags", comment = "微信粉丝标签表", uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"tag_id", "authorizer_appid", "company_id"})})
public class WechatTags {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 标签id */
    @MpField(value = "tag_id", columnType = "bigint", comment = "标签id")
    private Long tagId;

    /** 公众号appid */
    @MpField(value = "authorizer_appid", columnType = "string", length = 64, comment = "公众号appid")
    private String authorizerAppid;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 标签名称 */
    @MpField(value = "tag_name", columnType = "string", length = 50, comment = "标签名称")
    private String tagName;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
