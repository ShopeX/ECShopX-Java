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
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.repository.MerchantRepository;
import org.springframework.stereotype.Service;

@Service
public class MerchantDisabledUpdateService {

	private final MerchantRepository merchantRepository;

	public MerchantDisabledUpdateService(MerchantRepository merchantRepository) {
		this.merchantRepository = merchantRepository;
	}

	public void updateByMerchantId(long merchantId, Object rawDisabled) {
		if (rawDisabled == null) {
			throw new ResourceException("禁用状态必填");
		}
		boolean disabledBool;
		if (rawDisabled instanceof Boolean b) {
			disabledBool = b;
		} else {
			String norm = MerchantCreateParamNormalizer.normalizeScalarToString(rawDisabled);
			if (norm == null || norm.isBlank()) {
				throw new ResourceException("禁用状态必填");
			}
			if ("true".equals(norm) || "false".equals(norm)) {
				disabledBool = !"false".equals(norm);
			} else {
				throw new ResourceException("禁用状态必填");
			}
		}

		Merchant merchant = merchantRepository.findById(merchantId);
		if (merchant == null) {
			throw new ResourceException("未查询到更新数据");
		}
		merchantRepository.updateDisabledById(merchantId, disabledBool);
	}
}
