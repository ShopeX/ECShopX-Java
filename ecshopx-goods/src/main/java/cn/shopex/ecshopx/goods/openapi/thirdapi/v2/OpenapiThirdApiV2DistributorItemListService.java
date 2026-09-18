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

package cn.shopex.ecshopx.goods.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsRelListCoreService;
import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberTagListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2DistributorItemListService {

	private final DistributorMapper distributorMapper;
	private final OpenapiDistributorItemV2ListFilterBuilder filterBuilder;
	private final DistributorItemsRelListCoreService relListCore;
	private final OpenapiDistributorItemV2ListEnrichmentService enrichmentService;
	private final LangueProperties langueProperties;

	public OpenapiThirdApiV2DistributorItemListService(
			DistributorMapper distributorMapper,
			OpenapiDistributorItemV2ListFilterBuilder filterBuilder,
			DistributorItemsRelListCoreService relListCore,
			OpenapiDistributorItemV2ListEnrichmentService enrichmentService,
			LangueProperties langueProperties) {
		this.distributorMapper = distributorMapper;
		this.filterBuilder = filterBuilder;
		this.relListCore = relListCore;
		this.enrichmentService = enrichmentService;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> executeOpenapiList(
			long companyId,
			int page,
			int pageSize,
			String shopCodeRaw,
			String itemCodeRaw,
			String itemNameRaw,
			String goodsCanSaleRaw,
			String isTotalStoreRaw,
			String statusRaw,
			String distributorIdRaw) {
		Optional<Distributor> distributorOpt =
				findDistributor(companyId, shopCodeRaw, distributorIdRaw);
		if (distributorOpt.isEmpty()) {
			return OpenapiThirdApiV2MemberTagListService.formatListStruct(
					0, List.of(), page, pageSize);
		}

		Distributor entity = distributorOpt.get();
		long distributorId = entity.getDistributorId();

		OpenapiDistributorItemV2ListFilterBuilder.BuildResult buildResult =
				filterBuilder.build(
						companyId,
						itemCodeRaw,
						itemNameRaw,
						goodsCanSaleRaw,
						isTotalStoreRaw,
						statusRaw);
		if (buildResult.emptyEarly()) {
			return OpenapiThirdApiV2MemberTagListService.formatListStruct(
					0, List.of(), page, pageSize);
		}

		Map<String, Object> filter = buildResult.filter();
		filter.put("distributor_id", distributorId);

		Map<String, Object> coreOut =
				relListCore.queryOpenapiSpuPage(
						companyId,
						distributorId,
						filter,
						pageSize,
						page,
						langueProperties.getDefaultLang());

		long totalCount = longVal(coreOut.get("total_count"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> rows = (List<Map<String, Object>>) coreOut.get("list");

		if (rows != null && !rows.isEmpty()) {
			enrichmentService.apply(companyId, distributorId, rows);
		}

		return OpenapiThirdApiV2MemberTagListService.formatListStruct(
				totalCount, rows != null ? rows : List.of(), page, pageSize);
	}

	private Optional<Distributor> findDistributor(
			long companyId, String shopCodeRaw, String distributorIdRaw) {
		if (distributorIdRaw != null) {
			try {
				long id = Long.parseLong(distributorIdRaw.trim());
				return Optional.ofNullable(
						distributorMapper.selectOne(
								new LambdaQueryWrapper<Distributor>()
										.eq(Distributor::getCompanyId, companyId)
										.eq(Distributor::getDistributorId, id)
										.last("LIMIT 1")));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
		if (shopCodeRaw != null) {
			return Optional.ofNullable(
					distributorMapper.selectOne(
							new LambdaQueryWrapper<Distributor>()
									.eq(Distributor::getCompanyId, companyId)
									.eq(Distributor::getShopCode, shopCodeRaw)
									.last("LIMIT 1")));
		}
		return Optional.empty();
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
