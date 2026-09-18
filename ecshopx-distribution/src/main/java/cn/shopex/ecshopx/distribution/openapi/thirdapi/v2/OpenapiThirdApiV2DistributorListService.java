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

package cn.shopex.ecshopx.distribution.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.openapi.OpenapiDistributorOpenApiRowFormatSupport;
import cn.shopex.ecshopx.distribution.service.DistributorListOutsideLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2DistributorListService {

	private final DistributorMapper distributorMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorListOutsideLangReadService distributorListOutsideLangReadService;
	private final LangueProperties langueProperties;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2DistributorListService(
			DistributorMapper distributorMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorListOutsideLangReadService distributorListOutsideLangReadService,
			LangueProperties langueProperties,
			ObjectMapper objectMapper) {
		this.distributorMapper = distributorMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorListOutsideLangReadService = distributorListOutsideLangReadService;
		this.langueProperties = langueProperties;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiList(
			long companyId,
			int page,
			int pageSize,
			String shopCodeRaw,
			String distributorNameRaw,
			String statusRaw,
			String provinceRaw,
			String cityRaw,
			String areaRaw,
			String contactUsernameRaw,
			String contactMobileRaw,
			String distributorIdRaw) {
		LambdaQueryWrapper<Distributor> countWrapper =
				new LambdaQueryWrapper<Distributor>().eq(Distributor::getCompanyId, companyId);
		applyOpenapiListFilters(
				countWrapper,
				shopCodeRaw,
				distributorNameRaw,
				statusRaw,
				provinceRaw,
				cityRaw,
				areaRaw,
				contactUsernameRaw,
				contactMobileRaw,
				distributorIdRaw);

		long totalCount = distributorMapper.selectCount(countWrapper);

		LambdaQueryWrapper<Distributor> listWrapper =
				new LambdaQueryWrapper<Distributor>().eq(Distributor::getCompanyId, companyId);
		applyOpenapiListFilters(
				listWrapper,
				shopCodeRaw,
				distributorNameRaw,
				statusRaw,
				provinceRaw,
				cityRaw,
				areaRaw,
				contactUsernameRaw,
				contactMobileRaw,
				distributorIdRaw);
		listWrapper.orderByDesc(Distributor::getDistributorId);

		List<Distributor> entities = distributorMapper.selectList(listWrapper);

		List<Map<String, Object>> list;
		if (entities.isEmpty()) {
			list = List.of();
		} else {
			List<Map<String, Object>> overlayRows =
					entities.stream()
							.map(
									e ->
											OpenapiDistributorOpenApiRowFormatSupport.toInternalRowMap(
													e, sensitiveFieldEncryptor, objectMapper))
							.toList();
			String requestLang = langueProperties.getDefaultLang();
			distributorListOutsideLangReadService.overlayOpenapiListFields(
					companyId, requestLang, overlayRows);
			list =
					overlayRows.stream()
							.map(OpenapiDistributorOpenApiRowFormatSupport::toOpenapiRow)
							.toList();
		}

		return formatListStruct(totalCount, list, page, pageSize);
	}

	private static void applyOpenapiListFilters(
			LambdaQueryWrapper<Distributor> wrapper,
			String shopCodeRaw,
			String distributorNameRaw,
			String statusRaw,
			String provinceRaw,
			String cityRaw,
			String areaRaw,
			String contactUsernameRaw,
			String contactMobileRaw,
			String distributorIdRaw) {
		if (isPresentNonEmpty(shopCodeRaw)) {
			wrapper.eq(Distributor::getShopCode, shopCodeRaw);
		}
		if (isPresentNonEmpty(distributorIdRaw)) {
			try {
				long id = Long.parseLong(distributorIdRaw.trim());
				wrapper.eq(Distributor::getDistributorId, id);
			} catch (NumberFormatException e) {
				wrapper.eq(Distributor::getDistributorId, -1L);
			}
		}
		if (isPresentNonEmpty(distributorNameRaw)) {
			wrapper.like(Distributor::getName, distributorNameRaw);
		}
		applyStatusFilter(wrapper, statusRaw);
		if (isPresentNonEmpty(provinceRaw)) {
			wrapper.like(Distributor::getProvince, provinceRaw);
		}
		if (isPresentNonEmpty(cityRaw)) {
			wrapper.like(Distributor::getCity, cityRaw);
		}
		if (isPresentNonEmpty(areaRaw)) {
			wrapper.like(Distributor::getArea, areaRaw);
		}
		if (isPresentNonEmpty(contactUsernameRaw)) {
			wrapper.like(Distributor::getContact, contactUsernameRaw);
		}
		if (isPresentNonEmpty(contactMobileRaw)) {
			wrapper.eq(Distributor::getContractPhone, contactMobileRaw);
		}
	}

	private static void applyStatusFilter(LambdaQueryWrapper<Distributor> wrapper, String statusRaw) {
		if (!isPresentNonEmpty(statusRaw)) {
			return;
		}
		String isValid = mapStatusToIsValidLoose(statusRaw);
		if (isValid != null) {
			wrapper.eq(Distributor::getIsValid, isValid);
		}
	}

	private static String mapStatusToIsValidLoose(String statusRaw) {
		String trimmed = statusRaw.trim();
		try {
			int status;
			if (trimmed.contains(".")) {
				status = (int) Double.parseDouble(trimmed);
			} else {
				status = Integer.parseInt(trimmed);
			}
			switch (status) {
				case 0:
					return "delete";
				case 1:
					return "true";
				case 2:
					return "false";
				default:
					return null;
			}
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean isPresentNonEmpty(String raw) {
		return raw != null && !raw.isEmpty();
	}

	private static Map<String, Object> formatListStruct(
			long totalCount, List<Map<String, Object>> list, int page, int pageSize) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("is_last_page", computeIsLastPage(totalCount, page, pageSize));
		result.put("pager", Map.of("page", page, "page_size", pageSize));
		result.put("list", list != null ? list : List.of());
		return result;
	}

	private static int computeIsLastPage(long totalCount, int page, int pageSize) {
		if (pageSize <= 0) {
			return 1;
		}
		long totalPage = (long) Math.ceil((double) totalCount / pageSize);
		return totalPage <= page ? 1 : 0;
	}
}
