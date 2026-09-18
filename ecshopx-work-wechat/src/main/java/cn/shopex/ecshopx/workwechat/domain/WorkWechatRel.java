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

package cn.shopex.ecshopx.workwechat.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 企业微信用户关联表 */
@Data
@MpTable(value = "work_wechat_rel", comment = "企业微信用户关联表", indexes = {@MpIndex(name = "idx_company_id_user_id", columns = {"company_id", "user_id"})})
public class WorkWechatRel {

    /** 企业微信用户关联表id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "企业微信用户关联表id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 通讯录成员id */
    @MpField(value = "work_userid", columnType = "string", nullable = true, comment = "通讯录成员id")
    private String workUserid = "";

    /** 导购员id */
    @MpField(value = "salesperson_id", columnType = "bigint", nullable = true, comment = "导购员id", defaultValue = "0")
    private Long salespersonId = 0L;

    /** 企业微信外部成员id */
    @MpField(value = "external_userid", columnType = "string", nullable = true, comment = "企业微信外部成员id")
    private String externalUserid = "";

    /** 企业微信外部成员unionid */
    @MpField(value = "unionid", columnType = "string", nullable = true, comment = "企业微信外部成员unionid")
    private String unionid = "";

    /** 会员id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "会员id", defaultValue = "0")
    private Long userId = 0L;

    /** 是否好友 0 否 1 是 */
    @MpField(value = "is_friend", columnType = "boolean", nullable = true, comment = "是否好友 0 否 1 是", defaultValue = "0")
    private Boolean isFriend = false;

    /** 是否绑定 0 否 1 是 */
    @MpField(value = "is_bind", columnType = "boolean", nullable = true, comment = "是否绑定 0 否 1 是", defaultValue = "0")
    private Boolean isBind = false;

    /** 绑定时间 */
    @MpField(value = "bound_time", columnType = "bigint", nullable = true, comment = "绑定时间", defaultValue = "0")
    private Long boundTime = 0L;

    /** 添加好友时间 */
    @MpField(value = "add_friend_time", columnType = "bigint", nullable = true, comment = "添加好友时间", defaultValue = "0")
    private Long addFriendTime;
}
