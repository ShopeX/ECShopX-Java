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

package cn.shopex.ecshopx.pointsmall.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallItemsBatchStatusUpdateService {

	private static final String STATUS_VALIDATION_MSG = "状态必填,且必须是 onsale 或 instock ";

	private final PointsmallItemsMapper pointsmallItemsMapper;
	private final ObjectMapper objectMapper;

	public PointsmallItemsBatchStatusUpdateService(PointsmallItemsMapper pointsmallItemsMapper, ObjectMapper objectMapper) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
		this.objectMapper = objectMapper;
	}

	public void updateFromMerged(long companyId, Map<String, Object> merged) {
		List<Map<String, Object>> itemsList = resolveItemsList(merged.get("items"));
		String status = validateStatus(merged.get("status"));
		int now = (int) (System.currentTimeMillis() / 1000L);
		for (Map<String, Object> row : itemsList) {
			long goodsId = parseLongGoodsId(row.get("goods_id"));
			LambdaUpdateWrapper<PointsmallItems> u = new LambdaUpdateWrapper<PointsmallItems>()
					.eq(PointsmallItems::getCompanyId, companyId)
					.eq(PointsmallItems::getGoodsId, goodsId)
					.set(PointsmallItems::getApproveStatus, status)
					.set(PointsmallItems::getUpdated, now);
			pointsmallItemsMapper.update(null, u);
		}
	}

	private List<Map<String, Object>> resolveItemsList(Object raw) {
		if (raw == null) {
			throw new BadRequestException("未指定商品");
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException("未指定商品");
			}
			Object decoded;
			try {
				decoded = objectMapper.readValue(t, Object.class);
			} catch (Exception e) {
				throw new BadRequestException("items 格式错误");
			}
			if (!(decoded instanceof List<?> list)) {
				throw new BadRequestException("items 格式错误");
			}
			if (list.isEmpty()) {
				return List.of();
			}
			return normalizeItemsList(list);
		}
		if (raw instanceof Collection<?> col) {
			if (col.isEmpty()) {
				throw new BadRequestException("未指定商品");
			}
			return normalizeItemsList(new ArrayList<>(col));
		}
		throw new BadRequestException("items 格式错误");
	}

	private static List<Map<String, Object>> normalizeItemsList(List<?> list) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> m)) {
				throw new BadRequestException("商品id必填");
			}
			Map<String, Object> row = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				row.put(String.valueOf(e.getKey()), e.getValue());
			}
			out.add(row);
		}
		return out;
	}

	private static String validateStatus(Object statusRaw) {
		if (statusRaw == null) {
			throw new BadRequestException(STATUS_VALIDATION_MSG);
		}
		String s = statusRaw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(STATUS_VALIDATION_MSG);
		}
		if (!"onsale".equals(s) && !"instock".equals(s)) {
			throw new BadRequestException(STATUS_VALIDATION_MSG);
		}
		return s;
	}

	private static long parseLongGoodsId(Object g) {
		if (g == null) {
			throw new BadRequestException("商品id必填");
		}
		if (g instanceof Number n) {
			return n.longValue();
		}
		if (g instanceof String gs) {
			if (!StringUtils.hasText(gs.trim())) {
				throw new BadRequestException("商品id必填");
			}
		}
		String s = g.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("商品id必填");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("商品id必填");
		}
	}
}
