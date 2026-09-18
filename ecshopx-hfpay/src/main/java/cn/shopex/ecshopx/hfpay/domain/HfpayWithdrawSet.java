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

package cn.shopex.ecshopx.hfpay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 提现设置表
 */
@Data
@MpTable(value = "hfpay_withdraw_set", comment = "提现设置表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class HfpayWithdrawSet {

    /** 汇付提现配置表id */
    @MpId(value = "hfpay_withdraw_set_id", type = IdType.AUTO, columnType = "bigint", comment = "汇付提现配置表id")
    private Long hfpayWithdrawSetId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /**
     * 提现方式 1自动提现 2手动提现，默认 1
     */
    @MpField(value = "withdraw_method", columnType = "integer", comment = "提现方式 1自动提现 2手动提现", defaultValue = "1")
    private Integer withdrawMethod = 1;

    /** 店铺账号提现金额，默认 0 */
    @MpField(value = "distributor_money", columnType = "string", comment = "店铺账号提现金额", defaultValue = "0")
    private String distributorMoney = "0";

    /** 创建时间 */
    @MpField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @MpField("updated_at")
    private LocalDateTime updatedAt;
}
