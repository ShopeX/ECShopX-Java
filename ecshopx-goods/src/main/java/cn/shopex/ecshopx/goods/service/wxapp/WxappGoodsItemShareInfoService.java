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

package cn.shopex.ecshopx.goods.service.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGoodsItemShareInfoService {

	private final ItemsRepository itemsRepository;
	private final ObjectMapper objectMapper;

	public WxappGoodsItemShareInfoService(ItemsRepository itemsRepository, ObjectMapper objectMapper) {
		this.itemsRepository = itemsRepository;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> buildSharePayload(long companyId, long itemId) {
		Items row = itemsRepository.getSimpleInfoForWxappShare(companyId, itemId);
		if (row == null) {
			throw new ResourceException("商品不存在或者已下架");
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("item_id", row.getItemId() == null ? null : row.getItemId().longValue());
		out.put("item_name", row.getItemName());
		out.put("brief", row.getBrief());
		out.put("price", row.getPrice() == null ? null : row.getPrice().longValue());
		out.put("pics", buildWxappSharePics(row.getPics(), row.getPicsCreateQrcode()));
		return out;
	}

	private List<Map<String, Object>> buildWxappSharePics(String picsJson, String picsCreateQrcodeJson) {
		Object resolved = GoodsItemsListRowMapper.resolvePicsForListRow(picsJson);
		List<String> urls;
		if (resolved instanceof List<?> list) {
			urls = new ArrayList<>(list.size());
			for (Object o : list) {
				urls.add(o == null ? "" : String.valueOf(o));
			}
		} else if (resolved instanceof String s && StringUtils.hasText(s)) {
			urls = List.of(s);
		} else {
			urls = List.of();
		}

		JsonNode flagsArray = null;
		if (StringUtils.hasText(picsCreateQrcodeJson)) {
			try {
				JsonNode root = objectMapper.readTree(picsCreateQrcodeJson.trim());
				if (root != null && root.isArray()) {
					flagsArray = root;
				}
			} catch (Exception ignored) {
				// 非数组或解析失败：下标对齐不可用，isCode 一律 false
			}
		}

		List<Map<String, Object>> result = new ArrayList<>(urls.size());
		for (int i = 0; i < urls.size(); i++) {
			String url = urls.get(i);
			boolean isCode = false;
			if (flagsArray != null) {
				JsonNode flagNode = flagsArray.get(i);
				isCode = isWxappShareQrcodeFlagTrue(flagNode);
			}
			LinkedHashMap<String, Object> pic = new LinkedHashMap<>();
			pic.put("url", url);
			pic.put("isCode", isCode);
			result.add(pic);
		}
		return result;
	}

	private static boolean isWxappShareQrcodeFlagTrue(JsonNode flagNode) {
		if (flagNode == null || flagNode.isNull() || flagNode.isMissingNode()) {
			return false;
		}
		if (flagNode.isTextual()) {
			return "true".equals(flagNode.asText());
		}
		if (flagNode.isBoolean()) {
			return flagNode.booleanValue();
		}
		return false;
	}
}
