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

package cn.shopex.ecshopx.popularize.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 推广员提现表 */
@Data
@MpTable(value = "popularize_cash_withdrawal", comment = "推广员提现表")
public class PromoterCashWithdrawal {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 推广员userId */
    @MpField(value = "user_id", columnType = "string", comment = "推广员userId")
    private String userId;

    /** 提现账号姓名 */
    @MpField(value = "account_name", columnType = "string", nullable = true, comment = "提现账号姓名")
    private String accountName;

    /** 提现账号 微信为openid 支付宝为，支付账号 */
    @MpField(value = "pay_account", columnType = "string", comment = "提现账号 微信为openid 支付宝为，支付账号")
    private String payAccount;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", comment = "手机号")
    private String mobile;

    /** 提现金额，以分为单位 */
    @MpField(value = "money", columnType = "integer", comment = "提现金额，以分为单位", defaultValue = "0")
    private Integer money = 0;

    /** 提现状态 */
    @MpField(value = "status", columnType = "string", comment = "提现状态")
    private String status;

    /** 备注 */
    @MpField(value = "remarks", columnType = "string", nullable = true, comment = "备注")
    private String remarks;

    /** 提现支付类型 */
    @MpField(value = "pay_type", columnType = "string", comment = "提现支付类型")
    private String payType;

    /** 提现的小程序appid */
    @MpField(value = "wxa_appid", columnType = "string", comment = "提现的小程序appid")
    private String wxaAppid;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Integer updated;
}
