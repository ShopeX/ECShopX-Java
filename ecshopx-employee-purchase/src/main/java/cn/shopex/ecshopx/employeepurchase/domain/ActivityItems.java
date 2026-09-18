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

package cn.shopex.ecshopx.employeepurchase.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 员工内购商品表 */
@Data
@MpTable(value = "employee_purchase_activity_items", comment = "员工内购商品表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_activity_id", columns = {"activity_id"}), @MpIndex(name = "idx_item_id", columns = {"item_id"})}, uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"activity_id", "item_id"})})
public class ActivityItems {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 活动ID */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动ID")
    private Long activityId;

    /** 商品ID */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品ID")
    private Long itemId;

    /** 商品ID */
    @MpField(value = "goods_id", columnType = "bigint", comment = "商品ID")
    private Long goodsId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 活动价,单位为‘分’ */
    @MpField(value = "activity_price", columnType = "integer", comment = "活动价,单位为‘分’")
    private Integer activityPrice;

    /** 活动库存数量 */
    @MpField(value = "activity_store", columnType = "integer", comment = "活动库存数量")
    private Integer activityStore;

    /** 每人限额，以分为单位 */
    @MpField(value = "limit_fee", columnType = "integer", comment = "每人限额，以分为单位", defaultValue = "0")
    private Integer limitFee = 0;

    /** 每人限购数量 */
    @MpField(value = "limit_num", columnType = "integer", comment = "每人限购数量", defaultValue = "0")
    private Integer limitNum = 0;

    /** 排序 */
    @MpField(value = "sort", columnType = "integer", comment = "排序", defaultValue = "0")
    private Integer sort = 0;

    /** 上下架状态:1上架,0下架 */
    @MpField(value = "shelf_status", columnType = "smallint", comment = "上下架状态:1上架,0下架", defaultValue = "1")
    private Integer shelfStatus = 1;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
