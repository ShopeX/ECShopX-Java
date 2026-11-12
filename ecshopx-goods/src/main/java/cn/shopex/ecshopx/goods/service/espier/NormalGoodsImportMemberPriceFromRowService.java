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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.goods.service.memberprice.MemberPriceSaveService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 对齐 PHP {@code NormalGoodsUploadService::_saveMemberPrice}。
 */
@Service
public class NormalGoodsImportMemberPriceFromRowService {

	private final ObjectMapper objectMapper;
	private final MemberPriceSaveService memberPriceSaveService;

	public NormalGoodsImportMemberPriceFromRowService(
			ObjectMapper objectMapper, MemberPriceSaveService memberPriceSaveService) {
		this.objectMapper = objectMapper;
		this.memberPriceSaveService = memberPriceSaveService;
	}

	public void saveFromImportRowIfPresent(long companyId, long itemId, Map<String, Object> row) {
		if (companyId <= 0L || itemId <= 0L || row == null || row.isEmpty()) {
			return;
		}
		String mpriceJson = buildMpriceJson(itemId, row).orElse(null);
		if (mpriceJson == null) {
			return;
		}
		memberPriceSaveService.saveMemberPrice(companyId, String.valueOf(itemId), mpriceJson);
	}

	java.util.Optional<String> buildMpriceJson(long itemId, Map<String, Object> row) {
		ObjectNode grade = objectMapper.createObjectNode();
		ObjectNode vipGrade = objectMapper.createObjectNode();
		boolean priceValid = false;
		for (Map.Entry<String, Object> e : row.entrySet()) {
			String key = e.getKey();
			if (key == null) {
				continue;
			}
			if (key.startsWith("grade_price")) {
				String tierId = key.substring("grade_price".length());
				String yuan = normalizeYuan(e.getValue());
				if (hasPositiveYuan(yuan)) {
					priceValid = true;
				}
				grade.put(tierId, yuan == null ? "" : yuan);
			} else if (key.startsWith("vipGrade_price")) {
				String tierId = key.substring("vipGrade_price".length());
				String yuan = normalizeYuan(e.getValue());
				if (hasPositiveYuan(yuan)) {
					priceValid = true;
				}
				vipGrade.put(tierId, yuan == null ? "" : yuan);
			}
		}
		if (!priceValid) {
			return java.util.Optional.empty();
		}
		ObjectNode perItem = objectMapper.createObjectNode();
		perItem.set("grade", grade);
		perItem.set("vipGrade", vipGrade);
		ObjectNode root = objectMapper.createObjectNode();
		root.set(String.valueOf(itemId), perItem);
		try {
			return java.util.Optional.of(objectMapper.writeValueAsString(root));
		} catch (JsonProcessingException e) {
			return java.util.Optional.empty();
		}
	}

	private static String normalizeYuan(Object raw) {
		if (raw == null) {
			return "";
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return "";
		}
		try {
			double v = Double.parseDouble(s);
			if (v <= 0D) {
				return "";
			}
			return s;
		} catch (NumberFormatException e) {
			return "";
		}
	}

	private static boolean hasPositiveYuan(String yuan) {
		return StringUtils.hasText(yuan);
	}
}
