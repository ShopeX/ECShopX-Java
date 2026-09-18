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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.merchant.domain.MerchantSettlementApply;
import cn.shopex.ecshopx.merchant.mapper.MerchantSettlementApplyMapper;
import cn.shopex.ecshopx.merchant.repository.MerchantRepository;
import cn.shopex.ecshopx.merchant.repository.MerchantTypeRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantTypeDeleteService {

	private final MerchantTypeRepository merchantTypeRepository;
	private final MerchantSettlementApplyMapper merchantSettlementApplyMapper;
	private final MerchantRepository merchantRepository;

	public MerchantTypeDeleteService(
			MerchantTypeRepository merchantTypeRepository,
			MerchantSettlementApplyMapper merchantSettlementApplyMapper,
			MerchantRepository merchantRepository) {
		this.merchantTypeRepository = merchantTypeRepository;
		this.merchantSettlementApplyMapper = merchantSettlementApplyMapper;
		this.merchantRepository = merchantRepository;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteMerchantType(long companyId, long typeId) {
		List<Long> typeIds = buildTypeIdsForOccupancyCheck(companyId, typeId);

		LambdaQueryWrapper<MerchantSettlementApply> w1 = new LambdaQueryWrapper<>();
		w1.eq(MerchantSettlementApply::getCompanyId, companyId).in(MerchantSettlementApply::getMerchantTypeId, typeIds);
		if (merchantSettlementApplyMapper.selectCount(w1) > 0) {
			throw new ResourceException("该分类下有商家或有流程中的商家，请核实后再试");
		}

		if (merchantRepository.existsByCompanyIdAndMerchantTypeIdIn(companyId, typeIds)) {
			throw new ResourceException("该分类下有商家或有流程中的商家，请核实后再试");
		}

		boolean result = merchantTypeRepository.deleteByCompanyIdAndId(companyId, typeId);

		long childCount = merchantTypeRepository.countByParentIdAndCompanyId(companyId, typeId);
		boolean resultChild = true;
		if (childCount > 0) {
			resultChild = merchantTypeRepository.deleteByCompanyIdAndParentId(companyId, typeId);
		}

		if (!(result && resultChild)) {
			throw new ResourceException("删除失败");
		}
	}

	private List<Long> buildTypeIdsForOccupancyCheck(long companyId, long typeId) {
		List<Long> ids = new ArrayList<>();
		ids.add(typeId);
		List<Long> childIds = merchantTypeRepository.listIdsByParentIdAndCompanyId(companyId, typeId);
		ids.addAll(childIds);
		return ids;
	}
}
