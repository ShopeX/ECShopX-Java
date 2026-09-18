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

package cn.shopex.ecshopx.companys.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 导购员购物车
 */
@Data
@MpTable(value = "companys_operator_cart", comment = "导购员购物车", indexes = {@MpIndex(name = "idx_item_id", columns = {"item_id"}), @MpIndex(name = "idx_operator_id", columns = {"operator_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class OperatorCart {

    /** 购物车ID */
    @MpId(value = "cart_id", type = IdType.AUTO, columnType = "bigint", comment = "购物车ID")
    private Long cartId;

    /** 企业id */
    @MpField(value = "company_id", columnType = "bigint", comment = "企业id")
    private Long companyId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 管理员id */
    @MpField(value = "operator_id", columnType = "bigint", comment = "管理员id")
    private Long operatorId;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 商品数量 */
    @MpField(value = "num", columnType = "bigint", comment = "商品数量", defaultValue = "1")
    private Long num = 1L;

    /** 购物车是否选中 */
    @MpField("is_checked")
    private Boolean isChecked = true;

    /** 商品特殊类型 drug 处方药 normal 普通商品 */
    @MpField(value = "special_type", columnType = "string", comment = "商品特殊类型 drug 处方药 normal 普通商品", defaultValue = "normal")
    private String specialType = "normal";
}
