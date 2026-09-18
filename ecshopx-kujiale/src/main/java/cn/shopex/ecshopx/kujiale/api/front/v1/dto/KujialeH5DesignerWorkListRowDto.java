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

package cn.shopex.ecshopx.kujiale.api.front.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class KujialeH5DesignerWorkListRowDto {

	@JsonProperty("id")
	private Object id;

	@JsonProperty("design_name")
	private Object designName;

	@JsonProperty("cover_pic")
	private Object coverPic;

	@JsonProperty("is_origin")
	private Object isOrigin;

	@JsonProperty("is_excellent")
	private Object isExcellent;

	@JsonProperty("is_real_excellent")
	private Object isRealExcellent;

	@JsonProperty("is_top")
	private Object isTop;

	@JsonProperty("design_id")
	private Object designId;

	@JsonProperty("plan_id")
	private Object planId;

	@JsonProperty("comm_name")
	private Object commName;

	@JsonProperty("city")
	private Object city;

	@JsonProperty("name")
	private Object name;

	@JsonProperty("tag_id")
	private Object tagId;

	@JsonProperty("design_pano_url")
	private Object designPanoUrl;

	@JsonProperty("user_avatar")
	private Object userAvatar;

	@JsonProperty("email")
	private Object email;

	@JsonProperty("user_name")
	private Object userName;

	@JsonProperty("user_id")
	private Object userId;

	@JsonProperty("organization_id")
	private Object organizationId;

	@JsonProperty("created")
	private Object created;

	@JsonProperty("updated")
	private Object updated;

	@JsonProperty("view_count")
	private Object viewCount;

	@JsonProperty("like_count")
	private Object likeCount;

	@JsonProperty("ku_created")
	private Object kuCreated;

	@JsonProperty("is_like")
	private boolean isLike;

	@JsonProperty("taginfo")
	private List<Map<String, Object>> taginfo = new ArrayList<>();
}
