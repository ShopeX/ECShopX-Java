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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorListRowFormatService;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import cn.shopex.ecshopx.salesperson.domain.SalespersonTask;
import cn.shopex.ecshopx.salesperson.domain.SalespersonTaskRelDistributor;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskMapper;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonTaskRelDistributorMapper;
import cn.shopex.ecshopx.salesperson.service.support.SalespersonTaskFieldMaps;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SalespersonTaskInfoService {

	private final SalespersonTaskMapper salespersonTaskMapper;
	private final SalespersonTaskRelDistributorMapper salespersonTaskRelDistributorMapper;
	private final DistributorListQueryService distributorListQueryService;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final ObjectMapper objectMapper;

	public SalespersonTaskInfoService(
			SalespersonTaskMapper salespersonTaskMapper,
			SalespersonTaskRelDistributorMapper salespersonTaskRelDistributorMapper,
			DistributorListQueryService distributorListQueryService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			ObjectMapper objectMapper) {
		this.salespersonTaskMapper = salespersonTaskMapper;
		this.salespersonTaskRelDistributorMapper = salespersonTaskRelDistributorMapper;
		this.distributorListQueryService = distributorListQueryService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> info(long companyId, long taskId) {
		LambdaQueryWrapper<SalespersonTask> taskW = new LambdaQueryWrapper<>();
		taskW.eq(SalespersonTask::getTaskId, taskId).eq(SalespersonTask::getCompanyId, companyId);
		SalespersonTask taskEntity = salespersonTaskMapper.selectOne(taskW);

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		if (taskEntity != null) {
			result.putAll(SalespersonTaskFieldMaps.toDetailFields(taskEntity, objectMapper));
		}

		List<Map<String, Object>> distributorInfo = new ArrayList<>();
		List<Long> distributorIdList = new ArrayList<>();
		result.put("distributor_info", distributorInfo);
		result.put("distributor_id", distributorIdList);

		LambdaQueryWrapper<SalespersonTaskRelDistributor> relW = new LambdaQueryWrapper<>();
		relW.eq(SalespersonTaskRelDistributor::getTaskId, taskId)
				.eq(SalespersonTaskRelDistributor::getCompanyId, companyId)
				.orderByDesc(SalespersonTaskRelDistributor::getDistributorId);
		Page<SalespersonTaskRelDistributor> page = new Page<>(1, 1000);
		Page<SalespersonTaskRelDistributor> relPage = salespersonTaskRelDistributorMapper.selectPage(page, relW);

		List<Long> relIds = new ArrayList<>();
		for (SalespersonTaskRelDistributor rel : relPage.getRecords()) {
			if (rel.getDistributorId() != null) {
				relIds.add(rel.getDistributorId());
			}
		}

		if (relIds.isEmpty()) {
			return result;
		}

		List<Distributor> distributors = distributorListQueryService.listByIdsAndCompany(companyId, relIds);
		distributors.sort(Comparator.comparing(Distributor::getCreated, Comparator.nullsLast(Comparator.reverseOrder())));

		if (distributors.isEmpty()) {
			return result;
		}

		for (Distributor d : distributors) {
			long did = d.getDistributorId() != null ? d.getDistributorId() : 0L;
			int ds = d.getDistributorSelf() != null ? d.getDistributorSelf() : 0;
			Map<String, Object> setting = selfDeliverySettingReadService.getSetting(companyId, did, ds);
			Map<String, Object> row = distributorListRowFormatService.formatStoreRow(d, setting, objectMapper);
			distributorInfo.add(row);
			distributorIdList.add(did);
		}

		return result;
	}
}
