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

package cn.shopex.ecshopx.distribution.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.DistributorListOutsideLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1WxappShopListService {

	private static final Logger log = LoggerFactory.getLogger(OpenapiThirdApiV1WxappShopListService.class);

	private final DistributorMapper distributorMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorListOutsideLangReadService distributorListOutsideLangReadService;
	private final LangueProperties langueProperties;

	public OpenapiThirdApiV1WxappShopListService(
			DistributorMapper distributorMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorListOutsideLangReadService distributorListOutsideLangReadService,
			LangueProperties langueProperties) {
		this.distributorMapper = distributorMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorListOutsideLangReadService = distributorListOutsideLangReadService;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> execute(
			long companyId, int page, int pageSize, Long updatedGt, Long updatedLt) {
		log.debug(
				"daogouapi - weshop/list - input : companyId={}, page={}, pageSize={}, updatedGt={}, updatedLt={}",
				companyId,
				page,
				pageSize,
				updatedGt,
				updatedLt);

		LambdaQueryWrapper<Distributor> countWrapper = buildQueryWrapper(companyId, updatedGt, updatedLt);
		Long totalCount = distributorMapper.selectCount(countWrapper);

		LambdaQueryWrapper<Distributor> listWrapper = buildQueryWrapper(companyId, updatedGt, updatedLt);
		listWrapper
				.orderByAsc(Distributor::getCreated)
				.last("LIMIT " + pageSize + " OFFSET " + (long) (page - 1) * pageSize);
		List<Distributor> distributors = distributorMapper.selectList(listWrapper);

		List<Map<String, Object>> rows = new ArrayList<>();
		for (Distributor distributor : distributors) {
			rows.add(toRowMap(distributor));
		}

		String requestLang = langueProperties.getDefaultLang();
		distributorListOutsideLangReadService.overlayOpenapiListFields(companyId, requestLang, rows);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount != null ? totalCount : 0L);

		if (rows.isEmpty()) {
			result.put("list", List.of());
			return result;
		}

		List<Map<String, Object>> openapiList = new ArrayList<>(rows.size());
		for (Map<String, Object> row : rows) {
			openapiList.add(toOpenapiRow(row));
		}
		result.put("list", openapiList);
		return result;
	}

	private static LambdaQueryWrapper<Distributor> buildQueryWrapper(
			long companyId, Long updatedGt, Long updatedLt) {
		LambdaQueryWrapper<Distributor> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Distributor::getCompanyId, companyId);
		if (updatedGt != null) {
			wrapper.gt(Distributor::getUpdated, updatedGt);
		}
		if (updatedLt != null) {
			wrapper.lt(Distributor::getUpdated, updatedLt);
		}
		return wrapper;
	}

	private Map<String, Object> toRowMap(Distributor distributor) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("distributor_id", distributor.getDistributorId());
		row.put("name", distributor.getName());
		row.put("shop_code", distributor.getShopCode());
		row.put("logo", distributor.getLogo());

		String mobile = distributor.getMobile();
		if (mobile != null) {
			mobile = sensitiveFieldEncryptor.decrypt(mobile);
		}
		row.put("mobile", mobile);

		String contact = distributor.getContact();
		if (contact != null) {
			sensitiveFieldEncryptor.decrypt(contact);
		}

		row.put("hour", distributor.getHour());
		row.put("lng", distributor.getLng());
		row.put("lat", distributor.getLat());
		row.put("province", distributor.getProvince());
		row.put("city", distributor.getCity());
		row.put("area", distributor.getArea());
		row.put("address", distributor.getAddress());
		row.put("is_valid", distributor.getIsValid());
		return row;
	}

	private static Map<String, Object> toOpenapiRow(Map<String, Object> row) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("wxShopId", row.get("distributor_id"));
		out.put("storeName", nullToEmpty(row.get("name")));
		out.put("shopBn", nullToEmpty(row.get("shop_code")));
		out.put("logo", row.get("logo"));
		out.put("contractPhone", nullToEmpty(row.get("mobile")));
		out.put("hour", row.get("hour"));
		out.put("lng", row.get("lng"));
		out.put("lat", row.get("lat"));
		out.put("address", concatAddress(row));
		out.put("is_deleted", "true".equals(String.valueOf(row.get("is_valid"))) ? "0" : "1");
		return out;
	}

	private static String nullToEmpty(Object value) {
		return value != null ? String.valueOf(value) : "";
	}

	private static String concatAddress(Map<String, Object> row) {
		return nullToEmpty(row.get("province"))
				+ nullToEmpty(row.get("city"))
				+ nullToEmpty(row.get("area"))
				+ nullToEmpty(row.get("address"));
	}
}
