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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.repository.DistributorAdminListFilter;
import cn.shopex.ecshopx.distribution.repository.DistributorAdminListRepository;
import cn.shopex.ecshopx.distribution.repository.DistributorInfoReadRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OfflineAftersalesDistributorListService {

	private final DistributorInfoReadRepository distributorInfoReadRepository;
	private final DistributorAdminListRepository distributorAdminListRepository;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final DistributorListOutsideLangReadService distributorListOutsideLangReadService;
	private final ObjectMapper objectMapper;

	public OfflineAftersalesDistributorListService(
			DistributorInfoReadRepository distributorInfoReadRepository,
			DistributorAdminListRepository distributorAdminListRepository,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			DistributorListOutsideLangReadService distributorListOutsideLangReadService,
			ObjectMapper objectMapper) {
		this.distributorInfoReadRepository = distributorInfoReadRepository;
		this.distributorAdminListRepository = distributorAdminListRepository;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.distributorListOutsideLangReadService = distributorListOutsideLangReadService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getOtherOfflineAftersalesDistributor(
			long companyId,
			long requestDistributorId,
			long requestMerchantId,
			String isSelectedRaw,
			String distributorName,
			int page,
			int pageSize,
			String requestLang) {
		boolean isSelectedMode = "1".equals(isSelectedRaw) || "true".equals(isSelectedRaw);

		long effectiveMerchantId = requestMerchantId;
		Distributor currentEntity = null;

		if (requestDistributorId > 0) {
			currentEntity =
					distributorInfoReadRepository
							.findByCompanyAndDistributorId(companyId, requestDistributorId, requestLang)
							.orElseThrow(() -> new ResourceException("店铺不存在或无效"));
			effectiveMerchantId =
					currentEntity.getMerchantId() != null ? currentEntity.getMerchantId() : 0L;
		}

		DistributorAdminListFilter f = new DistributorAdminListFilter();
		f.setCompanyId(companyId);
		f.setIsValid("true");
		f.setOfflineAftersalesOtherEq(1);
		f.setDistributorIdNeq(requestDistributorId);
		f.setMerchantIdEq(effectiveMerchantId);
		f.setIncludeMerchantJoin(Boolean.valueOf(effectiveMerchantId == 0L));
		f.setRequestLang(requestLang);
		if (distributorName != null && !distributorName.trim().isEmpty()) {
			f.setNameContains(distributorName.trim());
		}
		if (requestDistributorId > 0 && isSelectedMode) {
			List<Long> linked = parseLinkedDistributorIds(currentEntity.getOfflineAftersalesDistributorId());
			if (!linked.isEmpty()) {
				f.setDistributorIdInConstraint(linked);
			} else {
				f.setForceEmptyResult(Boolean.TRUE);
			}
		}

		f.setOffset((page - 1) * pageSize);
		f.setLimit(pageSize);

		long total = distributorAdminListRepository.countByFilter(f);
		List<Distributor> entities = distributorAdminListRepository.selectPageByFilter(f);

		List<Map<String, Object>> list = new ArrayList<>();
		for (Distributor d : entities) {
			int distributorSelf = d.getDistributorSelf() == null ? 0 : d.getDistributorSelf().intValue();
			Map<String, Object> setting =
					selfDeliverySettingReadService.getSetting(companyId, d.getDistributorId(), distributorSelf);
			Map<String, Object> row =
					distributorListRowFormatService.formatStoreRow(d, setting, objectMapper);
			if (effectiveMerchantId > 0L) {
				row.remove("merchant_name");
			}
			list.add(row);
		}

		int totalCount = (int) Math.min(total, Integer.MAX_VALUE);
		if (requestDistributorId > 0 && page == 1 && currentEntity != null) {
			if (isSelectedMode) {
				boolean selfPin =
						currentEntity.getOfflineAftersalesSelf() != null
								&& currentEntity.getOfflineAftersalesSelf().intValue() != 0;
				if (selfPin) {
					list.add(0, formatPinnedRow(companyId, currentEntity, effectiveMerchantId, requestLang));
					totalCount++;
				}
			} else {
				list.add(0, formatPinnedRow(companyId, currentEntity, effectiveMerchantId, requestLang));
				totalCount++;
			}
		}

		distributorListOutsideLangReadService.applyLangMaps(companyId, requestLang, list);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("list", list);
		body.put("total_count", totalCount);
		return body;
	}

	private List<Long> parseLinkedDistributorIds(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return new ArrayList<>();
		}
		String s = raw.trim();
		Object decoded;
		try {
			decoded = objectMapper.readValue(s, Object.class);
		} catch (JsonProcessingException e) {
			return new ArrayList<>();
		}
		if (decoded == null) {
			return new ArrayList<>();
		}
		if (decoded instanceof List<?> decodedList) {
			List<Long> out = new ArrayList<>();
			for (Object elem : decodedList) {
				if (elem == null) {
					continue;
				}
				if (elem instanceof Number n) {
					out.add(n.longValue());
					continue;
				}
				String t = elem.toString().trim();
				if (t.isEmpty()) {
					continue;
				}
				try {
					out.add(Long.parseLong(t));
				} catch (NumberFormatException ignored) {
					// skip invalid element
				}
			}
			return out;
		}
		if (decoded instanceof Number n) {
			return List.of(n.longValue());
		}
		return new ArrayList<>();
	}

	private Map<String, Object> formatPinnedRow(
			long companyId, Distributor current, long effectiveMerchantId, String requestLang) {
		int distributorSelf = current.getDistributorSelf() == null ? 0 : current.getDistributorSelf().intValue();
		Map<String, Object> setting =
				selfDeliverySettingReadService.getSetting(companyId, current.getDistributorId(), distributorSelf);
		Map<String, Object> row =
				distributorListRowFormatService.formatStoreRow(current, setting, objectMapper);
		if (effectiveMerchantId > 0L) {
			row.remove("merchant_name");
		}
		return row;
	}
}
