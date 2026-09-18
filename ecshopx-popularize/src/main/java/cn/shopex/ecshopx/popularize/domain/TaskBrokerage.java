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
@MpTable(value = "popularize_task_brokerage", comment = "任务制返佣")
public class TaskBrokerage {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    @MpField(value = "user_id", columnType = "bigint")
    private Long userId;

    /** 订单号 */
    @MpField(value = "order_id", columnType = "string", length = 64, nullable = true, comment = "订单号")
    private String orderId;

    @MpField(value = "buy_user_id", columnType = "bigint")
    private Long buyUserId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", nullable = true, comment = "商品名称")
    private String itemName;

    /** 商品规格描述 */
    @MpField(value = "item_spec_desc", columnType = "text", nullable = true, comment = "商品规格描述")
    private String itemSpecDesc;

    /** 价格 */
    @MpField(value = "price", columnType = "integer", comment = "价格")
    private Integer price;

    /** 销售数量 */
    @MpField(value = "num", columnType = "integer", comment = "销售数量")
    private Integer num;

    /** 状态 */
    @MpField(value = "status", columnType = "string", comment = "状态")
    private String status;

    /** 计划结算时间 */
    @MpField(value = "plan_date", columnType = "string", comment = "计划结算时间")
    private String planDate;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
