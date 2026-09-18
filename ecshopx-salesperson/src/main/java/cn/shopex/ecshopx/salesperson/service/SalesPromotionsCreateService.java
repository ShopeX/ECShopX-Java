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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.salesperson.repository.SalesPromotionsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class SalesPromotionsCreateService {

	private final SalesPromotionsRepository salesPromotionsRepository;
	private final ObjectMapper objectMapper;

	public SalesPromotionsCreateService(SalesPromotionsRepository salesPromotionsRepository, ObjectMapper objectMapper) {
		this.salesPromotionsRepository = salesPromotionsRepository;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> createSalesPromotions(
			long companyId, long salespersonId, long distributorId, List<Map<String, Object>> cartItem) {
		String uniqueKey = computeUniqueKey(cartItem);
		String promotionItemsJson;
		try {
			promotionItemsJson = objectMapper.writeValueAsString(cartItem);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("促销单参数格式错误");
		}
		if (salesPromotionsRepository
				.getInfo(companyId, distributorId, salespersonId, uniqueKey)
				.isPresent()) {
			return salesPromotionsRepository.updateOneBy(
					companyId, distributorId, salespersonId, uniqueKey, promotionItemsJson);
		}
		return salesPromotionsRepository.create(companyId, distributorId, salespersonId, uniqueKey, promotionItemsJson);
	}

	private static String computeUniqueKey(List<Map<String, Object>> cartItem) {
		List<Long> ids = new ArrayList<>(cartItem.size());
		for (Map<String, Object> row : cartItem) {
			ids.add(parseItemId(row.get("item_id")));
		}
		ids.sort(Comparator.naturalOrder());
		StringBuilder sb = new StringBuilder();
		for (Long id : ids) {
			sb.append(Long.toString(id));
		}
		return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static long parseItemId(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			throw new BadRequestException("商品 item_id 不能为空");
		}
		String s = v.toString().trim();
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("商品 item_id 格式错误");
		}
	}
}
