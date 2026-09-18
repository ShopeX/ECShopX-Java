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

import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.repository.MerchantRepository;
import org.springframework.stereotype.Service;

@Service
public class MerchantQueryService {

	private final MerchantRepository merchantRepository;

	public MerchantQueryService(MerchantRepository merchantRepository) {
		this.merchantRepository = merchantRepository;
	}

	public Merchant getInfo(Long companyId, Long merchantId, boolean disabled) {
		return merchantRepository.getInfo(companyId, merchantId, disabled);
	}
}
