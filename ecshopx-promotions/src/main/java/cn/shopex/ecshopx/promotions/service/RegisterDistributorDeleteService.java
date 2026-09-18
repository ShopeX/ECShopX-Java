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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.DistributorPromotions;
import cn.shopex.ecshopx.promotions.domain.RegisterPromotions;
import cn.shopex.ecshopx.promotions.mapper.DistributorPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.RegisterPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterDistributorDeleteService {

	private final RegisterPromotionsMapper registerPromotionsMapper;
	private final DistributorPromotionsMapper distributorPromotionsMapper;

	public RegisterDistributorDeleteService(
			RegisterPromotionsMapper registerPromotionsMapper,
			DistributorPromotionsMapper distributorPromotionsMapper) {
		this.registerPromotionsMapper = registerPromotionsMapper;
		this.distributorPromotionsMapper = distributorPromotionsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteRegister(long companyId, String idPath) {
		if (idPath == null || idPath.trim().isEmpty()) {
			throw new ResourceException("删除的数据不存在", 500);
		}
		String t = idPath.trim();
		try {
			Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("删除的数据不存在", 500);
		}
		long promotionId = Long.parseLong(t);

		LambdaQueryWrapper<RegisterPromotions> w1 =
				new LambdaQueryWrapper<RegisterPromotions>()
						.eq(RegisterPromotions::getCompanyId, companyId)
						.eq(RegisterPromotions::getRegisterType, "distributor")
						.eq(RegisterPromotions::getId, promotionId);
		int deleted = registerPromotionsMapper.delete(w1);
		if (deleted == 0) {
			throw new ResourceException("删除的数据不存在", 500);
		}

		LambdaQueryWrapper<DistributorPromotions> w2 =
				new LambdaQueryWrapper<DistributorPromotions>()
						.eq(DistributorPromotions::getCompanyId, companyId)
						.eq(DistributorPromotions::getPromotionId, promotionId)
						.eq(DistributorPromotions::getPromotionType, "register");
		distributorPromotionsMapper.delete(w2);
	}
}
