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

/** 优惠券赠送表 */
@Data
@MpTable(value = "coupon_give_log", comment = "优惠券赠送表")
public class CouponGiveLog {

    /** 优惠券赠送失败记录id */
    @MpId(value = "give_log_id", type = IdType.AUTO, columnType = "bigint", comment = "优惠券赠送失败记录id")
    private Long giveLogId;

    /** 商户id */
    @MpField(value = "company_id", columnType = "bigint", comment = "商户id")
    private Long companyId;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 发送者 */
    @MpField(value = "sender", columnType = "string", comment = "发送者")
    private String sender;

    /** 赠送数量 */
    @MpField(value = "number", columnType = "bigint", comment = "赠送数量")
    private Long number;

    /** 失败数量 */
    @MpField(value = "error", columnType = "bigint", comment = "失败数量")
    private Long error;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
