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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.DistributorListRowFormatService;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import cn.shopex.ecshopx.promotions.domain.DistributorPromotions;
import cn.shopex.ecshopx.promotions.mapper.DistributorPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PromotionRegisterDistributorListService {

	private static final int PAGE = 1;
	private static final int PAGE_SIZE = 100;

	private final DistributorPromotionsMapper distributorPromotionsMapper;
	private final DistributorMapper distributorMapper;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final ObjectMapper objectMapper;

	public PromotionRegisterDistributorListService(
			DistributorPromotionsMapper distributorPromotionsMapper,
			DistributorMapper distributorMapper,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			ObjectMapper objectMapper) {
		this.distributorPromotionsMapper = distributorPromotionsMapper;
		this.distributorMapper = distributorMapper;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getDistributorList(long companyId, HttpServletRequest request) {
		Long excludeId = null;
		if (request.getParameterMap().containsKey("id")) {
			excludeId = parseFirstLongId(request);
		}

		LambdaQueryWrapper<DistributorPromotions> relW = new LambdaQueryWrapper<>();
		relW.eq(DistributorPromotions::getCompanyId, companyId)
				.eq(DistributorPromotions::getPromotionType, "register");
		if (excludeId != null) {
			relW.ne(DistributorPromotions::getPromotionId, excludeId);
		}
		List<DistributorPromotions> relData = distributorPromotionsMapper.selectList(relW);

		List<Long> notIn = List.of();
		if (relData != null && !relData.isEmpty()) {
			notIn =
					relData.stream()
							.map(DistributorPromotions::getDistributorId)
							.filter(Objects::nonNull)
							.distinct()
							.toList();
		}

		LambdaQueryWrapper<Distributor> base = new LambdaQueryWrapper<>();
		base.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getIsValid, "true")
				.orderByDesc(Distributor::getCreated);
		if (!notIn.isEmpty()) {
			base.notIn(Distributor::getDistributorId, notIn);
		}

		long countLong = distributorMapper.selectCount(base);
		int total = Math.toIntExact(Math.min((long) Integer.MAX_VALUE, countLong));

		int offset = (PAGE - 1) * PAGE_SIZE;
		LambdaQueryWrapper<Distributor> baseWithLimit = base.clone();
		baseWithLimit.last("LIMIT " + offset + ", " + PAGE_SIZE);
		List<Distributor> entities = distributorMapper.selectList(baseWithLimit);

		List<Map<String, Object>> list = new ArrayList<>();
		for (Distributor d : entities) {
			long did = d.getDistributorId() == null ? 0L : d.getDistributorId();
			int ds = d.getDistributorSelf() == null ? 0 : d.getDistributorSelf();
			Map<String, Object> setting = selfDeliverySettingReadService.getSetting(companyId, did, ds);
			Map<String, Object> row =
					distributorListRowFormatService.formatStoreRow(d, setting, objectMapper);
			list.add(row);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", total);
		return out;
	}

	private static Long parseFirstLongId(HttpServletRequest request) {
		String[] values = request.getParameterValues("id");
		if (values == null) {
			return null;
		}
		for (String raw : values) {
			if (raw == null) {
				continue;
			}
			String t = raw.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}
}
