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

package cn.shopex.ecshopx.goods.service.hotword;

import cn.shopex.ecshopx.goods.domain.SearchHotword;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 热词表行 → 接口字段（ECX-11390）。
 *
 * <p>admin 列表/详情额外返回三态状态与展示时间串；前台查询复用 {@link #toPublicRow}，不暴露状态与创建时间。
 * {@code jump_page} 返回 JSON 字符串（与会员中心一致），端上再 {@code JSON.parse} / decode。
 */
public final class SearchHotwordRows {

	private SearchHotwordRows() {}

	/** admin 列表/详情行：含 {@code status}/{@code status_name} 与时间展示串。 */
	public static Map<String, Object> toAdminRow(SearchHotword row, long nowEpochSecond) {
		Map<String, Object> out = toPublicRow(row);
		int status = SearchHotwordStatus.resolve(row.getStartTime(), row.getEndTime(), nowEpochSecond);
		out.put("status", status);
		out.put("status_name", SearchHotwordStatus.name(status));
		out.put("start_time_text", SearchHotwordTime.format(row.getStartTime()));
		out.put("end_time_text", SearchHotwordTime.format(row.getEndTime()));
		out.put("created", row.getCreated());
		out.put("created_text", SearchHotwordTime.format(row.getCreated()));
		out.put("updated", row.getUpdated());
		return out;
	}

	/** 前台展示行：只包含渲染热词所需字段。 */
	public static Map<String, Object> toPublicRow(SearchHotword row) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", row.getId());
		out.put("content", row.getContent());
		out.put("type", row.getType() == null ? SearchHotword.TYPE_TEXT : row.getType());
		out.put("text_color", row.getTextColor());
		out.put("background_color", row.getBackgroundColor());
		out.put("icon_url", row.getIconUrl());
		out.put("image_url", row.getImageUrl());
		out.put("sort", row.getSort() == null ? Integer.valueOf(0) : row.getSort());
		out.put("channel", SearchHotwordChannels.toResponseChannel(row.getChannel()));
		out.put("jump_page", SearchHotwordJumpPage.toResponseValue(row.getJumpPage()));
		out.put("start_time", row.getStartTime());
		out.put("end_time", row.getEndTime());
		return out;
	}
}
