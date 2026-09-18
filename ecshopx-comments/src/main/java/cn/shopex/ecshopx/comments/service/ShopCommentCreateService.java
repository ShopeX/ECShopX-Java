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
import cn.shopex.ecshopx.comments.mapper.ShopCommentsMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopCommentCreateService {

	private static final TypeReference<List<Object>> PICS_LIST_TYPE = new TypeReference<>() {};

	private final ShopCommentsMapper shopCommentsMapper;
	private final ObjectMapper objectMapper;

	public ShopCommentCreateService(ShopCommentsMapper shopCommentsMapper, ObjectMapper objectMapper) {
		this.shopCommentsMapper = shopCommentsMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> create(long companyId, Map<String, Object> merged) {
		Object uidRaw = merged.get("user_id");
		if (uidRaw == null) {
			throw new BadRequestException("缺少必填字段: user_id");
		}
		String userIdStr = uidRaw.toString().trim();
		if (!StringUtils.hasText(userIdStr)) {
			throw new BadRequestException("缺少必填字段: user_id");
		}

		Object companyRaw = merged.get("company_id");
		String companyIdStr = companyRaw != null ? companyRaw.toString() : String.valueOf(companyId);

		Object contentRaw = merged.get("content");
		String content = contentRaw != null ? contentRaw.toString() : "";
		int contentBytes = content.getBytes(StandardCharsets.UTF_8).length;
		if (contentBytes < 15) {
			throw new ResourceException("评论内容必须大于5个汉字或15个字母！");
		}
		if (contentBytes > 15000) {
			throw new ResourceException("评论内容不超过500个汉字！");
		}

		if (!merged.containsKey("pics")) {
			throw new BadRequestException("缺少必填字段: pics");
		}

		List<Object> picsList = normalizePics(merged.get("pics"));
		if (picsList.size() > 3) {
			throw new ResourceException("最多添加三张评论图片！");
		}

		String picsJson;
		try {
			picsJson = objectMapper.writeValueAsString(picsList);
		} catch (Exception e) {
			throw new BadRequestException("pics 参数无效");
		}

		ShopComments entity = new ShopComments();
		entity.setCompanyId(companyIdStr);
		entity.setUserId(userIdStr);
		Object shopIdObj = merged.get("shop_id");
		entity.setShopId(shopIdObj != null && StringUtils.hasText(shopIdObj.toString()) ? shopIdObj.toString() : "0");
		entity.setContent(content);
		entity.setPics(picsJson);
		entity.setStuck(false);
		entity.setHid(false);
		entity.setIsReply(false);
		Object sourceObj = merged.get("source");
		if (sourceObj != null && StringUtils.hasText(sourceObj.toString())) {
			entity.setSource(sourceObj.toString());
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreated(now);
		entity.setUpdated(now);

		shopCommentsMapper.insert(entity);

		return ShopCommentResponseMaps.buildCommentResponseMap(entity, objectMapper);
	}

	private List<Object> normalizePics(Object raw) {
		if (raw == null) {
			return new ArrayList<>();
		}
		if (raw instanceof List<?> list) {
			List<Object> out = new ArrayList<>();
			for (Object o : list) {
				out.add(o);
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			try {
				List<Object> parsed = objectMapper.readValue(s, PICS_LIST_TYPE);
				return parsed != null ? new ArrayList<>(parsed) : new ArrayList<>();
			} catch (Exception e) {
				return new ArrayList<>();
			}
		}
		return new ArrayList<>();
	}
}
