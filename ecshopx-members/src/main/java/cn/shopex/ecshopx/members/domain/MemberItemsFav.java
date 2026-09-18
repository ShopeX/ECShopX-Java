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

package cn.shopex.ecshopx.members.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 会员收藏商品表 */
@Data
@MpTable(value = "members_items_fav", comment = "会员收藏商品表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_user_id", columns = {"user_id"})})
public class MemberItemsFav {

    /** id */
    @MpId(value = "fav_id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long favId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 会员id */
    @MpField(value = "user_id", columnType = "bigint", comment = "会员id")
    private Long userId;

    /** 商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品id")
    private Long itemId;

    /** 商品名称 */
    @MpField(value = "item_name", columnType = "string", length = 100, comment = "商品名称")
    private String itemName;

    /** 商品图片 */
    @MpField(value = "item_image", columnType = "text", comment = "商品图片")
    private String itemImage;

    /** 价格,单位为‘分’ */
    @MpField(value = "item_price", columnType = "integer", comment = "价格,单位为‘分’")
    private Integer itemPrice;

    /** 商品类型，normal: 普通商品 pointsmall:积分商城 */
    @MpField(value = "item_type", columnType = "string", length = 15, comment = "商品类型，normal: 普通商品 pointsmall:积分商城", defaultValue = "normal")
    private String itemType = "normal";

    /** 积分兑换价格,item_type=pointsmall时必须 */
    @MpField(value = "point", columnType = "integer", nullable = true, defaultValue = "0")
    private Integer point = 0;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
