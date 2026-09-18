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

package cn.shopex.ecshopx.kujiale.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 设计师作品与商品绑定关系表 */
@Data
@MpTable(value = "kujiale_designer_works_item_rel", comment = "设计师作品与商品绑定关系表", indexes = {@MpIndex(name = "idx_item_id", columns = {"item_id"}), @MpIndex(name = "idx_design_id", columns = {"design_id"}), @MpIndex(name = "idx_goods_bn", columns = {"goods_bn"})})
public class KujialeDesignerWorksItemRel {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 商品ID */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品ID")
    private Long itemId;

    /** 设计ID */
    @MpField(value = "design_id", columnType = "string", length = 255, comment = "设计ID")
    private String designId;

    /** SPU货号 */
    @MpField(value = "goods_bn", columnType = "string", length = 255, nullable = true, comment = "SPU货号")
    private String goodsBn;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer", comment = "创建时间")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
