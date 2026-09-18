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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 优惠券核销记录表 */
@Data
@MpTable(value = "kaquan_user_discount_logs", comment = "优惠券核销记录表")
public class UserDiscountLogs {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", length = 64, comment = "自增id")
    private Long id;

    /** 用户的唯一标识 */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户的唯一标识")
    private Long userId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", comment = "手机号")
    private String mobile;

    /** 姓名 */
    @MpField(value = "username", columnType = "string", length = 500, nullable = true, comment = "姓名")
    private String username;

    /** 微信用户领取的卡券 id  */
    @MpField(value = "card_id", columnType = "bigint", length = 40, comment = "微信用户领取的卡券 id ")
    private Long cardId;

    /** 卡券 code 序列号 */
    @MpField(value = "code", columnType = "string", length = 30, comment = "卡券 code 序列号")
    private String code;

    /** 卡券名,最大9个汉字 */
    @MpField(value = "title", columnType = "string", length = 27, comment = "卡券名,最大9个汉字")
    private String title;

    /** 优惠券类型，可选值有 discount 折扣券;cash:代金券;gift:兑换券 */
    @MpField(value = "card_type", columnType = "string", comment = "优惠券类型，可选值有 discount 折扣券;cash:代金券;gift:兑换券")
    private String cardType;

    /** 核销卡券的门店名称 */
    @MpField(value = "shop_name", columnType = "string", nullable = true, comment = "核销卡券的门店名称")
    private String shopName;

    /** 核销时间 */
    @MpField(value = "used_time", columnType = "integer", comment = "核销时间")
    private Integer usedTime;

    /** 核销状态；consume:核销，callback:回退 */
    @MpField(value = "used_status", columnType = "string", nullable = true, comment = "核销状态；consume:核销，callback:回退")
    private String usedStatus = "consume";

    /** 核销订单 */
    @MpField(value = "used_order", columnType = "string", nullable = true, comment = "核销订单")
    private String usedOrder;
}
