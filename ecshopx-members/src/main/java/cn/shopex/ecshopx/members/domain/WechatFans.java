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

/** 微信粉丝表 */
@Data
@MpTable(value = "members_wechat_fans", comment = "微信粉丝表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_openid", columns = {"open_id"})})
public class WechatFans {

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 公众号appid */
    @MpField(value = "authorizer_appid", columnType = "string", length = 64, comment = "公众号appid")
    private String authorizerAppid;

    /** 是否订阅 */
    @MpField(value = "subscribed", columnType = "boolean", nullable = true, comment = "是否订阅")
    private Boolean subscribed;

    /** open_id */
    @MpField(value = "open_id", columnType = "string", length = 40, comment = "open_id")
    private String openId;

    /** 昵称 */
    @MpField(value = "nickname", columnType = "string", comment = "昵称")
    private String nickname;

    /** 性别。0 未知；1 男；2 女 */
    @MpField(value = "sex", columnType = "smallint", nullable = true, comment = "性别。0 未知；1 男；2 女")
    private Integer sex;

    /** 市 */
    @MpField(value = "city", columnType = "string", nullable = true, comment = "市")
    private String city;

    /** 国 */
    @MpField(value = "country", columnType = "string", nullable = true, comment = "国")
    private String country;

    /** 省 */
    @MpField(value = "province", columnType = "string", nullable = true, comment = "省")
    private String province;

    /** 语言 */
    @MpField(value = "language", columnType = "string", nullable = true, comment = "语言")
    private String language;

    @MpField(value = "headimgurl", columnType = "string", nullable = true)
    private String headimgurl;

    /** 订阅时间 */
    @MpField(value = "subscribe_time", columnType = "integer", nullable = true, comment = "订阅时间")
    private Integer subscribeTime;

    /** 第三方unionid */
    @MpId(value = "unionid", type = IdType.INPUT, columnType = "string", length = 40, comment = "第三方unionid")
    private String unionid;

    /** 备注 */
    @MpField(value = "remark", columnType = "string", nullable = true, comment = "备注")
    private String remark;

    @MpField(value = "groupid", columnType = "integer", nullable = true)
    private Integer groupid;

    @MpField(value = "tagids", columnType = "string", nullable = true)
    private String tagids;

    /** 列表标签弹出框所需字段 */
    @MpField(value = "tagpop", columnType = "boolean", comment = "列表标签弹出框所需字段", defaultValue = "False")
    private Boolean tagpop = false;

    /** 列表页备注弹出框所需字段 */
    @MpField(value = "remarkpop", columnType = "boolean", comment = "列表页备注弹出框所需字段", defaultValue = "False")
    private Boolean remarkpop = false;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
