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

/** 任务制返佣 */
@Data
@MpTable(value = "popularize_task_brokerage_count", comment = "任务制返佣")
public class TaskBrokerageCount {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 返佣模式 */
    @MpField(value = "rebate_type", columnType = "string", length = 15, comment = "返佣模式")
    private String rebateType;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 商品编号 */
    @MpField(value = "item_bn", columnType = "string", comment = "商品编号")
    private String itemBn;

    /** 已完成的总销售额 */
    @MpField(value = "total_fee", columnType = "bigint", comment = "已完成的总销售额")
    private Long totalFee;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", nullable = true, comment = "商品名称")
    private String itemName;

    /** 商品规格描述 */
    @MpField(value = "item_spec_desc", columnType = "text", nullable = true, comment = "商品规格描述")
    private String itemSpecDesc;

    @MpField(value = "user_id", columnType = "bigint")
    private Long userId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 分销配置 */
    @MpField(value = "rebate_conf", columnType = "json_array", nullable = true, comment = "分销配置")
    private String rebateConf;

    /** 分销奖金 */
    @MpField(value = "rebate_money", columnType = "bigint", comment = "分销奖金", defaultValue = "0")
    private Long rebateMoney = 0L;

    /** 订单已完成数量 */
    @MpField(value = "finish_num", columnType = "string", comment = "订单已完成数量")
    private String finishNum;

    /** 订单已支付，待完成数量 */
    @MpField(value = "wait_num", columnType = "string", comment = "订单已支付，待完成数量")
    private String waitNum;

    /** 订单已关闭数量，包含取消订单，售后订单 */
    @MpField(value = "close_num", columnType = "string", comment = "订单已关闭数量，包含取消订单，售后订单")
    private String closeNum;

    /** 计划结算时间 */
    @MpField(value = "plan_date", columnType = "string", comment = "计划结算时间")
    private String planDate;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
