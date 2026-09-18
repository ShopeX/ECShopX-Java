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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 购物车 */
@Data
@MpTable(value = "orders_cart", comment = "购物车", indexes = {@MpIndex(name = "idx_item_id", columns = {"item_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_promoter_user_id", columns = {"promoter_user_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_shop_type", columns = {"shop_type"}), @MpIndex(name = "idx_wxa_appid", columns = {"wxa_appid"}), @MpIndex(name = "idx_is_checked", columns = {"is_checked"})})
public class Cart {

    /** ID */
    @MpId(value = "cart_id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long cartId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 推广员user_id */
    @MpField(value = "promoter_user_id", columnType = "bigint", nullable = true, comment = "推广员user_id")
    private Long promoterUserId;

    /** 会员ident,会员信息和session生成的唯一值 */
    @MpField(value = "user_ident", columnType = "string", length = 50, nullable = true, comment = "会员ident,会员信息和session生成的唯一值")
    private String userIdent;

    /** 店铺类型；distributor:店铺，shop:门店，community:社区, mall:商城, drug 药品清单 */
    @MpField(value = "shop_type", columnType = "string", nullable = true, comment = "店铺类型；distributor:店铺，shop:门店，community:社区, mall:商城, drug 药品清单", defaultValue = "distributor")
    private String shopType = "distributor";

    /** 店铺id 或者 社区id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "店铺id 或者 社区id", defaultValue = "0")
    private Long shopId = 0L;

    /** 活动类型 */
    @MpField(value = "activity_type", nullable = true, comment = "活动类型")
    private String activityType;

    /** 活动id */
    @MpField(value = "activity_id", columnType = "bigint", nullable = true, comment = "活动id", defaultValue = "0")
    private Long activityId;

    /** 促销类型 */
    @MpField(value = "marketing_type", columnType = "string", nullable = true, comment = "促销类型")
    private String marketingType;

    /** 促销id */
    @MpField(value = "marketing_id", columnType = "bigint", nullable = true, comment = "促销id", defaultValue = "0")
    private Long marketingId;

    /** 商品类型。可选值有 normal 实体类商品;services 服务类商品, normal_gift:实体赠品,services_gift:服务类赠品 */
    @MpField(value = "item_type", columnType = "string", comment = "商品类型。可选值有 normal 实体类商品;services 服务类商品, normal_gift:实体赠品,services_gift:服务类赠品", defaultValue = "normal")
    private String itemType = "normal";

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 组合商品关联商品id */
    @MpField(value = "items_id", columnType = "string", nullable = true, comment = "组合商品关联商品id")
    private String itemsId;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", length = 255, nullable = true, comment = "商品名称")
    private String itemName;

    /** 图片 */
    @MpField(value = "pics", columnType = "string", length = 1024, nullable = true, comment = "图片")
    private String pics;

    /** 购买商品数量 */
    @MpField(value = "num", columnType = "integer", comment = "购买商品数量")
    private Integer num;

    /** 购买商品价格 */
    @MpField(value = "price", columnType = "integer", comment = "购买商品价格", defaultValue = "0")
    private Integer price = 0;

    /** 积分兑换价格 */
    @MpField(value = "point", columnType = "integer", nullable = true, comment = "积分兑换价格", defaultValue = "0")
    private Integer point = 0;

    /** 小程序appid */
    @MpField(value = "wxa_appid", columnType = "string", nullable = true, comment = "小程序appid")
    private String wxaAppid;

    /** 购物车是否选中 */
    @MpField(value = "is_checked", columnType = "boolean", comment = "购物车是否选中", defaultValue = "True")
    private Boolean isChecked = true;

    /** 是加价购商品 */
    @MpField(value = "is_plus_buy", columnType = "boolean", comment = "是加价购商品", defaultValue = "False")
    private Boolean isPlusBuy = false;

    /** 购物车数据来源：scancode：扫码加入购物车, normal:正常加入购物车,salesperson:业务员导购下单 */
    @MpField(value = "source_type", columnType = "string", comment = "购物车数据来源：scancode：扫码加入购物车, normal:正常加入购物车,salesperson:业务员导购下单", defaultValue = "normal")
    private String sourceType = "normal";

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
