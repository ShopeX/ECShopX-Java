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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.community.domain.CommunityChief;
import cn.shopex.ecshopx.community.domain.CommunityChiefDistributor;
import cn.shopex.ecshopx.community.mapper.CommunityChiefDistributorMapper;
import cn.shopex.ecshopx.community.mapper.CommunityChiefMapper;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorListRowFormatService;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CommunityChiefDistributorListQueryService {

	private final CommunityChiefMapper communityChiefMapper;
	private final CommunityChiefDistributorMapper communityChiefDistributorMapper;
	private final DistributorListQueryService distributorListQueryService;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final ObjectMapper objectMapper;

	public CommunityChiefDistributorListQueryService(
			CommunityChiefMapper communityChiefMapper,
			CommunityChiefDistributorMapper communityChiefDistributorMapper,
			DistributorListQueryService distributorListQueryService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			ObjectMapper objectMapper) {
		this.communityChiefMapper = communityChiefMapper;
		this.communityChiefDistributorMapper = communityChiefDistributorMapper;
		this.distributorListQueryService = distributorListQueryService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> listForChiefMember(long companyId, long memberUserId) {
		CommunityChief chief =
				communityChiefMapper.selectOne(
						new LambdaQueryWrapper<CommunityChief>()
								.eq(CommunityChief::getCompanyId, companyId)
								.eq(CommunityChief::getUserId, memberUserId)
								.last("LIMIT 1"));
		if (chief == null || chief.getChiefId() == null || chief.getChiefId() <= 0L) {
			throw new ResourceException("当前用户不是团长");
		}
		return buildFormattedDistributorRows(companyId, chief.getChiefId());
	}

	/**
	 * 按团长主键校验存在且属于该公司后，返回与团长店铺列表接口一致的格式化行（含平台自营行）。
	 */
	public List<Map<String, Object>> listFormattedDistributorRowsForChief(long companyId, long chiefId) {
		CommunityChief chief = communityChiefMapper.selectById(chiefId);
		if (chief == null || !Objects.equals(chief.getCompanyId(), companyId)) {
			throw new ResourceException("当前用户不是团长");
		}
		return buildFormattedDistributorRows(companyId, chiefId);
	}

	private List<Map<String, Object>> buildFormattedDistributorRows(long companyId, long chiefId) {
		List<CommunityChiefDistributor> binds =
				communityChiefDistributorMapper.selectList(
						new LambdaQueryWrapper<CommunityChiefDistributor>()
								.eq(CommunityChiefDistributor::getChiefId, chiefId)
								.orderByAsc(CommunityChiefDistributor::getId));

		List<Long> orderedIds = new ArrayList<>(binds.size());
		for (CommunityChiefDistributor row : binds) {
			Long normalized = row.getDistributorId() == null ? 0L : row.getDistributorId();
			orderedIds.add(normalized);
		}

		if (orderedIds.isEmpty()) {
			return Collections.emptyList();
		}

		boolean appendPlatform = orderedIds.contains(0L);
		List<Long> queryIds =
				orderedIds.stream().filter(id -> id > 0L).distinct().toList();

		List<Distributor> loaded =
				distributorListQueryService.listByIdsAndCompany(companyId, queryIds);
		Map<Long, Distributor> byId =
				loaded.stream()
						.collect(
								Collectors.toMap(
										Distributor::getDistributorId, Function.identity(), (a, b) -> a));

		List<Map<String, Object>> out = new ArrayList<>();
		for (Long id : orderedIds) {
			if (id == 0L) {
				continue;
			}
			Distributor d = byId.get(id);
			if (d == null) {
				continue;
			}
			int distributorSelf = d.getDistributorSelf() == null ? 0 : d.getDistributorSelf();
			Map<String, Object> selfDelivery =
					selfDeliverySettingReadService.getSetting(
							companyId, d.getDistributorId(), distributorSelf);
			out.add(
					distributorListRowFormatService.formatStoreRow(d, selfDelivery, objectMapper));
		}

		if (appendPlatform) {
			out.add(
					new LinkedHashMap<>(
							Map.of("distributor_id", 0L, "name", "平台自营")));
		}

		return out;
	}
}
