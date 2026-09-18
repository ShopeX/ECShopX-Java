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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 商品推荐表（已废弃：ECX-10013 改用 goods_recommend_* 规则表，停止读写 items_recommend）。
 *
 * @deprecated 见 docs/prd/商品推荐开发逻辑整理.md §8.4
 */
@Deprecated
@Data
@MpTable(value = "items_recommend", comment = "商品推荐表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_main_item_id", columns = {"main_item_id"})})
public class ItemsRecommend {

	/** id */
	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
	private Long id;

	/** 主商品 */
	@MpField(value = "main_item_id", columnType = "bigint", comment = "主商品")
	private Long mainItemId;

	/** 商品 */
	@MpField(value = "item_id", columnType = "bigint", comment = "商品")
	private Long itemId;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	/** 商品名称 */
	@MpField(value = "item_name", columnType = "string", comment = "商品名称")
	private String itemName;

	/** 简洁的描述 */
	@MpField(value = "brief", columnType = "string", length = 255, nullable = true, comment = "简洁的描述")
	private String brief;

	/** 商品图片 */
	@MpField(value = "pics", columnType = "text", comment = "商品图片")
	private String pics;

	/** 价格,单位为‘分’ */
	@MpField(value = "price", columnType = "integer", comment = "价格,单位为‘分’")
	private Integer price;

	/** 原价,单位为‘分’ */
	@MpField(value = "market_price", columnType = "integer", comment = "原价,单位为‘分’", defaultValue = "0")
	private Integer marketPrice = 0;

	/** 产品规格描述 */
	@MpField(value = "item_spec_desc", columnType = "string", nullable = true, comment = "产品规格描述")
	private String itemSpecDesc = "";

	/** 商品排序 */
	@MpField(value = "sort", columnType = "integer", comment = "商品排序", defaultValue = "0")
	private Integer sort = 0;
}
