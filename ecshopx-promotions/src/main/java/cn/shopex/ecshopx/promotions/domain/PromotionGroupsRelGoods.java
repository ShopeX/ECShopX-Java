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

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

@Data
@MpTable(
		value = "promotion_groups_rel_goods",
		comment = "拼团关联商品表",
		indexes = {
			@MpIndex(name = "ix_company_id", columns = {"company_id"}),
			@MpIndex(name = "ix_groups_item_id", columns = {"groups_activity_id", "item_id"}),
			@MpIndex(name = "ix_item_id", columns = {"item_id"})
		},
		uniqueIndexes = {
			@MpIndex(name = "uk_groups_item", columns = {"groups_activity_id", "item_id"})
		})
public class PromotionGroupsRelGoods {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "关联id")
	private Long id;

	@MpField(value = "groups_activity_id", columnType = "bigint", comment = "拼团活动id")
	private Long groupsActivityId;

	@MpField(value = "item_id", columnType = "bigint", comment = "SKU/货品id")
	private Long itemId;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "item_spec_desc", columnType = "string", nullable = true, comment = "商品规格描述")
	private String itemSpecDesc;

	@MpField(value = "item_title", columnType = "string", comment = "商品名称")
	private String itemTitle;

	@MpField(value = "item_pic", columnType = "text", nullable = true, comment = "商品图片")
	private String itemPic;

	@MpField(value = "activity_price", columnType = "bigint", comment = "拼团活动价格(分)", defaultValue = "0")
	private Long activityPrice = 0L;

	@MpField(value = "activity_store", columnType = "bigint", comment = "拼团活动库存", defaultValue = "0")
	private Long activityStore = 0L;

	@MpField(value = "sales_store", columnType = "bigint", comment = "已售库存", defaultValue = "0")
	private Long salesStore = 0L;

	@MpField(value = "is_show", columnType = "boolean", comment = "是否展示", defaultValue = "True")
	private Boolean isShow = true;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
