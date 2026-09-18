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
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.repository.MerchantRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class DistributorH5MerchantAvailableService {

	private final DistributorMapper distributorMapper;
	private final MerchantRepository merchantRepository;

	public DistributorH5MerchantAvailableService(
			DistributorMapper distributorMapper, MerchantRepository merchantRepository) {
		this.distributorMapper = distributorMapper;
		this.merchantRepository = merchantRepository;
	}

	public boolean merchantAvailableForDistributor(long companyId, long distributorId) {
		if (distributorId == 0L) {
			return true;
		}
		Distributor info =
				distributorMapper.selectOne(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, companyId)
								.eq(Distributor::getDistributorId, distributorId)
								.last("LIMIT 1"));
		if (info == null) {
			throw new ResourceException("店铺信息查询失败");
		}
		Long merchantId = info.getMerchantId();
		if (merchantId == null || merchantId == 0L) {
			return true;
		}
		Merchant merchant = merchantRepository.findById(merchantId);
		if (merchant == null) {
			return true;
		}
		return !merchant.isDisabled();
	}
}
