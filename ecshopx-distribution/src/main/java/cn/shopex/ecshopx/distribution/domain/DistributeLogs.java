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

/** 分销商佣金记录表 */
@Data
@MpTable(value = "distribution_distribute_logs", comment = "分销商佣金记录表")
public class DistributeLogs {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id")
    private Long distributorId;

    @MpField(value = "distributor_mobile", columnType = "string", length = 32)
    private String distributorMobile;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "bigint", length = 64, comment = "订单号")
    private Long orderId;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "门店id", defaultValue = "0")
    private Long shopId = 0L;

    /** 分销商手机号 */
    @MpField(value = "mobile", columnType = "string", length = 32, nullable = true, comment = "分销商手机号")
    private String mobile;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", nullable = true, comment = "商品名称")
    private String itemName;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", nullable = true, comment = "用户id")
    private Long userId;

    /** 商品图片 */
    @MpField(value = "pic", columnType = "string", nullable = true, comment = "商品图片")
    private String pic;

    /** 购买商品数量 */
    @MpField(value = "num", columnType = "integer", comment = "购买商品数量")
    private Integer num;

    /** 单个分销金额，以分为单位 */
    @MpField(value = "rebate", columnType = "integer", comment = "单个分销金额，以分为单位", defaultValue = "0")
    private Integer rebate = 0;

    /** 总分销金额，以分为单位 */
    @MpField(value = "total_rebate", columnType = "integer", comment = "总分销金额，以分为单位", defaultValue = "0")
    private Integer totalRebate = 0;

    /** 是否已结算 */
    @MpField(value = "is_close", columnType = "boolean", comment = "是否已结算", defaultValue = "False")
    private Boolean isClose = false;

    /** 计划结算时间 */
    @MpField(value = "plan_close_time", columnType = "integer", comment = "计划结算时间")
    private Integer planCloseTime;

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", comment = "订单更新时间")
    private Integer updateTime;
}
