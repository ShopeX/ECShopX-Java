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
import lombok.Data;

@Data
public class DesignerWorksListRowDto {

	@JsonProperty("id")
	private Object id;

	@JsonProperty("design_id")
	private String designId;

	@JsonProperty("design_name")
	private String designName;

	@JsonProperty("cover_pic")
	private Object coverPic;

	@JsonProperty("plan_id")
	private Object planId;

	@JsonProperty("comm_name")
	private Object commName;

	@JsonProperty("city")
	private Object city;

	@JsonProperty("name")
	private Object name;

	@JsonProperty("view_count")
	private int viewCount;

	@JsonProperty("like_count")
	private int likeCount;

	@JsonProperty("created")
	private Object created;

	@JsonProperty("updated")
	private Object updated;

	@JsonProperty("is_bound")
	private boolean bound;

	@JsonProperty("bound_item")
	private DesignerWorksListBoundItemDto boundItem;

	@Data
	public static class DesignerWorksListBoundItemDto {

		@JsonProperty("item_id")
		private Object itemId;

		@JsonProperty("item_name")
		private String itemName;

		@JsonProperty("item_bn")
		private String itemBn;

		@JsonProperty("goods_bn")
		private String goodsBn;
	}
}
