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

package cn.shopex.ecshopx.kujiale.api.admin.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class DesignerWorksItemListRowDto {

	@JsonProperty("item_id")
	private Object itemId;

	@JsonProperty("item_name")
	private String itemName;

	@JsonProperty("item_bn")
	private String itemBn;

	@JsonProperty("goods_bn")
	private String goodsBn;

	@JsonProperty("approve_status")
	private Object approveStatus;

	@JsonProperty("item_category")
	private Object itemCategory;

	@JsonProperty("stock")
	private Integer stock;

	@JsonProperty("itemCatName")
	private String itemCatName;

	@JsonProperty("tagList")
	private List<TagEntry> tagList;

	@JsonProperty("price")
	private Object price;

	@JsonProperty("market_price")
	private Object marketPrice;

	@JsonProperty("pics")
	private Object pics;

	@JsonProperty("created")
	private Object created;

	@JsonProperty("updated")
	private Object updated;

	@JsonProperty("design")
	private DesignBlock design;

	@JsonProperty("bind_info")
	private BindInfo bindInfo;

	@Data
	public static class TagEntry {

		@JsonProperty("tag_id")
		private Object tagId;

		@JsonProperty("tag_name")
		private String tagName;

		@JsonProperty("tag_color")
		private Object tagColor;

		@JsonProperty("font_color")
		private Object fontColor;
	}

	@Data
	public static class DesignBlock {

		@JsonProperty("design_id")
		private String designId;

		@JsonProperty("design_name")
		private String designName;

		@JsonProperty("cover_pic")
		private Object coverPic;

		@JsonProperty("tags")
		private String tags;
	}

	@Data
	public static class BindInfo {

		@JsonProperty("rel_id")
		private Object relId;

		@JsonProperty("bind_created")
		private Object bindCreated;
	}
}
