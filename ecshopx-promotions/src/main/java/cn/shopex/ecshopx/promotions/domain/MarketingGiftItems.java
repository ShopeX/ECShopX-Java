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

/** 活动赠品列表 */
@Data
@MpTable(value = "promotions_marketing_gift_items", comment = "活动赠品列表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_item_id", columns = {"item_id"}), @MpIndex(name = "ix_marketing_id", columns = {"marketing_id"})})
public class MarketingGiftItems {

    /** 自增id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "自增id")
    private Long id;

    /** 关联营销id */
    @MpField(value = "marketing_id", columnType = "bigint", comment = "关联营销id")
    private Long marketingId;

    /** 关联商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "关联商品id")
    private Long itemId;

    /** 活动商品类型: normal:实体类商品,service:服务类商品 */
    @MpField(value = "item_type", columnType = "string", comment = "活动商品类型: normal:实体类商品,service:服务类商品", defaultValue = "normal")
    private String itemType = "normal";

    /** 商品标题 */
    @MpField(value = "item_name", columnType = "string", length = 255, comment = "商品标题")
    private String itemName;

    /** 赠品价格 */
    @MpField(value = "price", columnType = "integer", comment = "赠品价格", defaultValue = "0")
    private Integer price = 0;

    /** 赠品库存 */
    @MpField(value = "store", columnType = "integer", comment = "赠品库存", defaultValue = "0")
    private Integer store = 0;

    /** 赠品数量 */
    @MpField(value = "gift_num", columnType = "integer", comment = "赠品数量", defaultValue = "0")
    private Integer giftNum = 1;

    /** 商品图片 */
    @MpField(value = "pics", columnType = "text", nullable = true, comment = "商品图片")
    private String pics;

    /** 退货无需退回赠品 */
    @MpField(value = "without_return", columnType = "boolean", comment = "退货无需退回赠品", defaultValue = "False")
    private Boolean withoutReturn = false;

    /** 营销条件标准  quantity:按总件数, totalfee:按总金额 */
    @MpField(value = "condition_type", columnType = "text", comment = "营销条件标准  quantity:按总件数, totalfee:按总金额", defaultValue = "totalfee")
    private String conditionType = "totalfee";

    /** 赠品满足所需条件 */
    @MpField(value = "filter_full", columnType = "integer", nullable = true, comment = "赠品满足所需条件")
    private Integer filterFull;

    /** 商品规格描述 */
    @MpField(value = "item_spec_desc", columnType = "string", nullable = true, comment = "商品规格描述")
    private String itemSpecDesc;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
