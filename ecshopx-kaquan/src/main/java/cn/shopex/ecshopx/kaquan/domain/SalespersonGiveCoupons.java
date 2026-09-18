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

/** 导购员发放给用户优惠券记录 */
@Data
@MpTable(value = "kaquan_salesperson_give_coupons", comment = "导购员发放给用户优惠券记录")
public class SalespersonGiveCoupons {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", length = 64, comment = "自增id")
    private Long id;

    /**  */
    @MpField(value = "company_id", columnType = "bigint", length = 64)
    private Long companyId;

    /** 导购员id */
    @MpField(value = "salesperson_id", columnType = "bigint", length = 64, comment = "导购员id")
    private Long salespersonId;

    /** 导购员名称 */
    @MpField(value = "salesperson_name", columnType = "string", comment = "导购员名称")
    private String salespersonName;

    /** 会员id */
    @MpField(value = "user_id", columnType = "bigint", comment = "会员id")
    private Long userId;

    /** 会员名称 */
    @MpField(value = "user_name", columnType = "string", comment = "会员名称")
    private String userName = "";

    /** 优惠券id */
    @MpField(value = "coupons_id", columnType = "bigint", comment = "优惠券id")
    private Long couponsId;

    /** 优惠券名称 */
    @MpField(value = "coupons_name", columnType = "string", comment = "优惠券名称")
    private String couponsName;

    /** 发送优惠券数量 */
    @MpField(value = "number", columnType = "integer", comment = "发送优惠券数量", defaultValue = "0")
    private Integer number = 0;

    /** 发放优惠券状态，1成功，0失败 */
    @MpField(value = "status", columnType = "integer", comment = "发放优惠券状态，1成功，0失败", defaultValue = "1")
    private Integer status = 1;

    /** 失败原因 */
    @MpField(value = "fail_reason", columnType = "string", nullable = true, comment = "失败原因")
    private String failReason;

    /** 发送优惠券时间 */
    @MpField(value = "give_time", columnType = "integer", comment = "发送优惠券时间")
    private Integer giveTime;

    /** 修改时间 */
    @MpField(value = "updated", columnType = "integer", comment = "修改时间")
    private Integer updated;
}
