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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 优惠券赠送失败记录表 */
@Data
@MpTable(value = "coupon_give_error_log", comment = "优惠券赠送失败记录表", indexes = {@MpIndex(name = "idx_companyid", columns = {"company_id"}), @MpIndex(name = "idx_giveid", columns = {"give_id"})})
public class CouponGiveErrorLog {

    /** 优惠券赠送失败记录id */
    @MpId(value = "give_log_id", type = IdType.AUTO, columnType = "bigint", comment = "优惠券赠送失败记录id")
    private Long giveLogId;

    /** 优惠券赠送失败记录id */
    @MpField(value = "give_id", columnType = "bigint", comment = "优惠券赠送失败记录id")
    private Long giveId;

    /** 赠送用户id */
    @MpField(value = "uid", columnType = "bigint", comment = "赠送用户id")
    private Long uid;

    /** 商户id */
    @MpField(value = "company_id", columnType = "bigint", comment = "商户id")
    private Long companyId;

    /** 赠送优惠券id */
    @MpField(value = "card_id", columnType = "bigint", comment = "赠送优惠券id")
    private Long cardId;

    /** 失败原因记录 */
    @MpField(value = "note", columnType = "string", comment = "失败原因记录")
    private String note;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
