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

/** 不同平台会员关联表，关联到members表 */
@Data
@MpTable(value = "members_associations", comment = "不同平台会员关联表，关联到members表", indexes = {@MpIndex(name = "ind_member_id_miss", columns = {"unionid", "company_id", "user_type"}), @MpIndex(name = "idx_company_userid_usertype", columns = {"company_id", "user_id", "user_type"})}, uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"user_id", "unionid", "company_id", "user_type"})})
public class MembersAssociations {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 第三方unionid */
    @MpField(value = "unionid", columnType = "string", length = 128, comment = "第三方unionid")
    private String unionid;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户类型：wechat/ali/apple/google/facebook/line 等 */
    @MpField(value = "user_type", columnType = "string", length = 30, comment = "用户类型，可选值有 wechat:微信;ali:支付宝;apple;google;facebook;line")
    private String userType;
}
