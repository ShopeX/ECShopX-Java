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

/** 粉丝关联标微信签表 */
@Data
@MpTable(value = "wechatfans_bind_wechattag", comment = "粉丝关联标微信签表", uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"tag_id", "open_id", "company_id"})})
public class WechatFansBindWechatTag {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 标签id */
    @MpField(value = "tag_id", columnType = "bigint", comment = "标签id")
    private Long tagId;

    /** open_id */
    @MpField(value = "open_id", columnType = "string", length = 40, comment = "open_id")
    private String openId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 公众号appid */
    @MpField(value = "authorizer_appid", columnType = "string", length = 64, comment = "公众号appid")
    private String authorizerAppid;
}
