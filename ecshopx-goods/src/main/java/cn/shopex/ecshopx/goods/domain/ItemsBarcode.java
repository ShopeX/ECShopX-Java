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

package cn.shopex.ecshopx.goods.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 商品条形码表
 */
@Data
@MpTable(
		value = "items_barcode",
		comment = "商品条形码表",
		indexes = {@MpIndex(name = "ix_item_id", columns = {"item_id"})},
		uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"company_id", "distributor_id", "barcode"})})
public class ItemsBarcode {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
	private Long id;

	/** 商品id */
	@MpField(value = "item_id", columnType = "bigint", length = 20, comment = "商品id")
	private Long itemId;

	/** 默认商品id */
	@MpField(value = "default_item_id", columnType = "bigint", length = 20, comment = "默认商品id")
	private Long defaultItemId;

	/** 公司id */
	@MpField(value = "company_id", columnType = "bigint", length = 20, comment = "公司id")
	private Long companyId;

	/** 店铺id，0=平台 */
	@MpField(value = "distributor_id", columnType = "bigint", length = 20, comment = "店铺id，0=平台", defaultValue = "0")
	private Long distributorId;

	/** 商品条形码 */
	@MpField(value = "barcode", columnType = "string", length = 50, comment = "商品条形码")
	private String barcode;
}
