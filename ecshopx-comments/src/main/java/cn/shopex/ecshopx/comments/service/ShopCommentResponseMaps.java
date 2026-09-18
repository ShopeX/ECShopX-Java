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

package cn.shopex.ecshopx.comments.service;

import cn.shopex.ecshopx.comments.domain.ShopComments;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class ShopCommentResponseMaps {

	private static final TypeReference<List<Object>> PICS_LIST_TYPE = new TypeReference<>() {};

	private ShopCommentResponseMaps() {
	}

	public static Map<String, Object> buildCommentResponseMap(ShopComments entity, ObjectMapper objectMapper) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("comment_id", entity.getCommentId());
		m.put("company_id", entity.getCompanyId());
		m.put("user_id", entity.getUserId());
		m.put("shop_id", entity.getShopId());
		m.put("content", entity.getContent());
		m.put("pics", parsePicsJson(entity.getPics(), objectMapper));
		m.put("stuck", Boolean.TRUE.equals(entity.getStuck()));
		m.put("hid", Boolean.TRUE.equals(entity.getHid()));
		m.put("source", entity.getSource());
		m.put("created", entity.getCreated());
		m.put("updated", entity.getUpdated());
		return m;
	}

	public static Map<String, Object> toListItemMap(ShopComments entity, ObjectMapper objectMapper) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("comment_id", entity.getCommentId());
		m.put("company_id", entity.getCompanyId());
		m.put("user_id", entity.getUserId());
		m.put("shop_id", entity.getShopId());
		m.put("content", entity.getContent());
		m.put("pics", parsePicsJson(entity.getPics(), objectMapper));
		m.put("is_reply", Boolean.TRUE.equals(entity.getIsReply()));
		m.put("reply_content", entity.getReplyContent());
		m.put("stuck", Boolean.TRUE.equals(entity.getStuck()));
		m.put("hid", Boolean.TRUE.equals(entity.getHid()));
		m.put("reply_time", entity.getReplyTime());
		m.put("created", entity.getCreated());
		m.put("updated", entity.getUpdated());
		return m;
	}

	private static Object parsePicsJson(String picsJson, ObjectMapper objectMapper) {
		if (picsJson == null || !StringUtils.hasText(picsJson)) {
			return null;
		}
		try {
			return objectMapper.readValue(picsJson, PICS_LIST_TYPE);
		} catch (Exception e) {
			return null;
		}
	}
}
