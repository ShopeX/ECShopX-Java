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

package cn.shopex.ecshopx.promotions.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 砍价日志表 */
@Data
@MpTable(value = "promotions_bargain_log", comment = "砍价日志表")
public class BargainLog {

    /** 砍价记录id */
    @MpId(value = "bargain_log_id", type = IdType.AUTO, columnType = "bigint", comment = "砍价记录id")
    private Long bargainLogId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 公众号的appid */
    @MpField(value = "authorizer_appid", columnType = "string", length = 64, nullable = true, comment = "公众号的appid")
    private String authorizerAppid;

    /** 小程序的appid */
    @MpField(value = "wxa_appid", columnType = "string", length = 64, nullable = true, comment = "小程序的appid")
    private String wxaAppid;

    /** 砍价ID */
    @MpField(value = "bargain_id", columnType = "bigint", comment = "砍价ID")
    private Long bargainId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 微信用户标识 */
    @MpField(value = "open_id", columnType = "string", length = 40, nullable = true, comment = "微信用户标识")
    private String openId;

    /** 用户昵称 */
    @MpField(value = "nickname", columnType = "string", nullable = true, comment = "用户昵称")
    private String nickname;

    /** 用户头像url */
    @MpField(value = "headimgurl", columnType = "string", nullable = true, comment = "用户头像url")
    private String headimgurl;

    /** 砍掉金额,单位为‘分’ */
    @MpField(value = "cutdown_num", columnType = "integer", comment = "砍掉金额,单位为‘分’")
    private Integer cutdownNum;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
