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

package cn.shopex.ecshopx.supplier.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商品佣金费率
 *
 * <p>索引：idx_goods_id（goods_id）
 */
@Data
@MpTable(value = "supplier_items_commission", comment = "商品佣金费率", indexes = {@MpIndex(name = "idx_goods_id", columns = {"goods_id"})})
public class SupplierItemsCommission {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 商户id */
    @MpField(value = "company_id", columnType = "bigint", comment = "商户id")
    private Long companyId;

    /** 商品 */
    @MpField(value = "item_id", columnType = "bigint", comment = "商品")
    private Long itemId;

    /** 产品ID */
    @MpField(value = "goods_id", columnType = "bigint", nullable = true, comment = "产品ID", defaultValue = "0")
    private Long goodsId = 0L;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "integer", comment = "供应商id", defaultValue = "0")
    private Integer supplierId = 0;

    /** 佣金比例 */
    @MpField(value = "commission_ratio", columnType = "integer", comment = "佣金比例", defaultValue = "0")
    private Integer commissionRatio = 0;

    /** 创建时间 */
    @MpField(value = "add_time", columnType = "datetime", nullable = true, comment = "创建时间")
    private LocalDateTime addTime;

    /** 更新时间 */
    @MpField(value = "modify_time", columnType = "datetime", nullable = true, comment = "更新时间")
    private LocalDateTime modifyTime;
}
