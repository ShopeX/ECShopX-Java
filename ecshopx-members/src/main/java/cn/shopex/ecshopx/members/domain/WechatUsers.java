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

/** 微信会员表 */
@Data
@MpTable(value = "members_wechatusers", comment = "微信会员表", indexes = {@MpIndex(name = "idx_unionid", columns = {"unionid"}), @MpIndex(name = "idx_openid", columns = {"open_id"}), @MpIndex(name = "idx_company_unionid", columns = {"company_id", "unionid"})}, uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"company_id", "authorizer_appid", "open_id", "unionid"})})
public class WechatUsers {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 小程序或者公众号appid */
    @MpField(value = "authorizer_appid", columnType = "string", length = 64, comment = "小程序或者公众号appid")
    private String authorizerAppid;

    /** open_id */
    @MpField(value = "open_id", columnType = "string", length = 40, comment = "open_id")
    private String openId;

    /** union_id */
    @MpField(value = "unionid", columnType = "string", length = 40, comment = "union_id")
    private String unionid;

    /** 昵称 */
    @MpField(value = "nickname", columnType = "string", length = 500, nullable = true, comment = "昵称")
    private String nickname;

    /** 头像url */
    @MpField(value = "headimgurl", columnType = "string", nullable = true, comment = "头像url")
    private String headimgurl;

    /** 来源id */
    @MpField(value = "inviter_id", columnType = "integer", nullable = true, comment = "来源id", defaultValue = "0")
    private Integer inviterId = 0;

    /** 来源类型 default默认 */
    @MpField(value = "source_from", columnType = "string", nullable = true, comment = "来源类型 default默认", defaultValue = "default")
    private String sourceFrom = "default";

    /** 是否需要迁移。0:不用迁移或迁移完成；1:需要迁移 */
    @MpField(value = "need_transfer", columnType = "boolean", comment = "是否需要迁移。0:不用迁移或迁移完成；1:需要迁移", defaultValue = "0")
    private Boolean needTransfer = false;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
