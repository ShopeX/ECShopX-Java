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
import cn.shopex.ecshopx.distribution.domain.PickupLocation;
import cn.shopex.ecshopx.distribution.mapper.PickupLocationMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PickupLocationRelDistributorService {

	private final PickupLocationMapper pickupLocationMapper;

	private final DistributorWriteRepository distributorWriteRepository;

	private final MessageSource messageSource;

	public PickupLocationRelDistributorService(
			PickupLocationMapper pickupLocationMapper,
			DistributorWriteRepository distributorWriteRepository,
			MessageSource messageSource) {
		this.pickupLocationMapper = pickupLocationMapper;
		this.distributorWriteRepository = distributorWriteRepository;
		this.messageSource = messageSource;
	}

	public void relDistributor(
			long companyId, long distributorScopeId, List<Long> pickupLocationIds, long relDistributorId) {
		Locale locale = LocaleContextHolder.getLocale();
		if (distributorScopeId > 0L && relDistributorId > 0L && distributorScopeId != relDistributorId) {
			throw new ResourceException(
					messageSource.getMessage("distribution.pickupLocation.onlyCurrentStoreRelation", null, locale));
		}
		Optional<Distributor> relDistributorRow = Optional.empty();
		if (relDistributorId > 0L) {
			relDistributorRow = distributorWriteRepository.selectSimpleByCompanyAndId(companyId, relDistributorId);
		}
		long updatedEpoch = System.currentTimeMillis() / 1000L;
		for (Long pickupId : pickupLocationIds) {
			LambdaQueryWrapper<PickupLocation> q = new LambdaQueryWrapper<>();
			q.eq(PickupLocation::getCompanyId, companyId)
					.eq(PickupLocation::getDistributorId, distributorScopeId)
					.eq(PickupLocation::getId, pickupId);
			PickupLocation row = pickupLocationMapper.selectOne(q);
			if (row == null) {
				throw new ResourceException(
						messageSource.getMessage("distribution.pickupLocation.pickupLocationNotExist", null, locale));
			}
			if (relDistributorId > 0L && relDistributorRow.isEmpty()) {
				throw new ResourceException(
						messageSource.getMessage("distribution.pickupLocation.relatedStoreNotExist", null, locale));
			}
			LambdaUpdateWrapper<PickupLocation> u = new LambdaUpdateWrapper<>();
			u.eq(PickupLocation::getCompanyId, companyId)
					.eq(PickupLocation::getDistributorId, distributorScopeId)
					.eq(PickupLocation::getId, pickupId)
					.set(PickupLocation::getRelDistributorId, relDistributorId)
					.set(PickupLocation::getUpdated, updatedEpoch);
			int affected = pickupLocationMapper.update(null, u);
			if (affected == 0) {
				throw new ResourceException(
						messageSource.getMessage("distribution.pickupLocation.noUpdateDataFound", null, locale));
			}
		}
	}

	public void cancelRelDistributor(
			long companyId,
			long distributorScopeId,
			List<Long> pickupLocationIds,
			long relDistributorIdWhere) {
		long updatedEpoch = System.currentTimeMillis() / 1000L;
		LambdaUpdateWrapper<PickupLocation> u = new LambdaUpdateWrapper<>();
		u.eq(PickupLocation::getCompanyId, companyId)
				.eq(PickupLocation::getDistributorId, distributorScopeId)
				.in(PickupLocation::getId, pickupLocationIds)
				.eq(PickupLocation::getRelDistributorId, relDistributorIdWhere)
				.set(PickupLocation::getRelDistributorId, 0L)
				.set(PickupLocation::getUpdated, updatedEpoch);
		pickupLocationMapper.update(null, u);
	}
}
