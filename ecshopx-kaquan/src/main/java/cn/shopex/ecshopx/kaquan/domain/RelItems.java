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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 卡券关联商品表 */
@Data
@MpTable(value = "kaquan_rel_items", comment = "卡券关联商品表", indexes = {@MpIndex(name = "idx_card_id", columns = {"card_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})}, uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"item_id", "card_id", "item_type"})})
public class RelItems {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 筛选id(按item_type区分为商品ID,标签ID等) */
    @MpField(value = "item_id", columnType = "bigint", length = 64, comment = "筛选id(按item_type区分为商品ID,标签ID等)")
    private Long itemId;

    /** 优惠券id */
    @MpField(value = "card_id", columnType = "bigint", comment = "优惠券id")
    private Long cardId;

    /** 筛选类型,可选值有normal:普通商品,tag:标签,brand:品牌,category:主类目 */
    @MpField(value = "item_type", columnType = "string", comment = "筛选类型,可选值有normal:普通商品,tag:标签,brand:品牌,category:主类目", defaultValue = "normal")
    private String itemType = "normal";

    /** 是否为列表默认展示 */
    @MpField(value = "is_show", columnType = "boolean", comment = "是否为列表默认展示", defaultValue = "True")
    private Boolean isShow = Boolean.TRUE;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long companyId;

    /** 兑换上限 */
    @MpField(value = "use_limit", columnType = "integer", comment = "兑换上限", defaultValue = "0")
    private Integer useLimit = 0;
}
