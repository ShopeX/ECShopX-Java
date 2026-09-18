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

/** 员工内购活动购物车 */
@Data
@MpTable(value = "employee_purchase_cart", comment = "员工内购活动购物车", indexes = {@MpIndex(name = "idx_enterprise_activity_item_id", columns = {"enterprise_id", "activity_id", "item_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class Cart {

    /** 购物车ID */
    @MpId(value = "cart_id", type = IdType.AUTO, columnType = "bigint", comment = "购物车ID")
    private Long cartId;

    /** 企业id */
    @MpField(value = "company_id", columnType = "bigint", comment = "企业id")
    private Long companyId;

    /** 企业id */
    @MpField(value = "enterprise_id", columnType = "bigint", comment = "企业id")
    private Long enterpriseId;

    /** 活动ID */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动ID")
    private Long activityId;

    /** 用户ID */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户ID")
    private Long userId;

    /** 店铺类型；distributor:店铺，shop:门店，community:社区, mall:商城, drug 药品清单 */
    @MpField(value = "shop_type", columnType = "string", nullable = true, comment = "店铺类型；distributor:店铺，shop:门店，community:社区, mall:商城, drug 药品清单", defaultValue = "distributor")
    private String shopType = "distributor";

    /** 店铺id 或者 社区id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "店铺id 或者 社区id", defaultValue = "0")
    private Long shopId = 0L;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 商品数量 */
    @MpField(value = "num", columnType = "bigint", comment = "商品数量", defaultValue = "1")
    private Long num = 1L;

    /** 购物车是否选中 */
    @MpField("is_checked")
    private Boolean isChecked = true;
}
