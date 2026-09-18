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

package cn.shopex.ecshopx.distribution.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 分销商佣金提现记录表 */
@Data
@MpTable(value = "distribution_cash_withdrawal", comment = "分销商佣金提现记录表")
public class CashWithdrawal {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "string", comment = "分销商id")
    private String distributorId;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "门店id", defaultValue = "0")
    private Long shopId = 0L;

    /** 分销商真实姓名 */
    @MpField(value = "distributor_name", columnType = "string", comment = "分销商真实姓名")
    private String distributorName;

    /** 分销商open_id */
    @MpField(value = "open_id", columnType = "string", comment = "分销商open_id")
    private String openId;

    /** 会员ID */
    @MpField(value = "user_id", columnType = "string", comment = "会员ID")
    private String userId;

    @MpField(value = "distributor_mobile", columnType = "string", length = 32)
    private String distributorMobile;

    /** 提现金额，以分为单位 */
    @MpField(value = "money", columnType = "integer", comment = "提现金额，以分为单位", defaultValue = "0")
    private Integer money = 0;

    /** 提现状态 */
    @MpField(value = "status", columnType = "string", comment = "提现状态")
    private String status;

    /** 备注 */
    @MpField(value = "remarks", columnType = "string", nullable = true, comment = "备注")
    private String remarks;

    /** 提现的小程序appid */
    @MpField(value = "wxa_appid", columnType = "string", comment = "提现的小程序appid")
    private String wxaAppid;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
