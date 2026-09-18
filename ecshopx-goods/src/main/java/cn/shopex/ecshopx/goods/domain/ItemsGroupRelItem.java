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
 * 商品分组关联商品
 */
@Data
@MpTable(value = "items_group_rel_item", comment = "商品分组关联商品", indexes = {@MpIndex(name = "idx_goods_id", columns = {"goods_id"}), @MpIndex(name = "idx_group_id", columns = {"group_id"})})
public class ItemsGroupRelItem {

	/** ID */
	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
	private Long id;

	/** 公司ID */
	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID", defaultValue = "0")
	private Long companyId = 0L;

	/** 地区ID */
	@MpField(value = "regionauth_id", columnType = "bigint", comment = "地区ID", defaultValue = "0")
	private Long regionauthId = 0L;

	/** 分组ID */
	@MpField(value = "group_id", columnType = "bigint", comment = "分组ID", defaultValue = "0")
	private Long groupId = 0L;

	/**
	 * 分组类型(coupon, widget, marketing)
	 */
	@MpField(value = "group_type", columnType = "string", length = 50, comment = "分组类型(coupon, widget, marketing)")
	private String groupType = "";

	/** sku-id */
	@MpField(value = "item_id", columnType = "bigint", comment = "sku-id", defaultValue = "0")
	private Long itemId = 0L;

	/** spu-id */
	@MpField(value = "goods_id", columnType = "bigint", comment = "spu-id", defaultValue = "0")
	private Long goodsId = 0L;

	/** 是否删除 */
	@MpField(value = "is_del", columnType = "bigint", comment = "是否删除", defaultValue = "0")
	private Long isDel = 0L;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
