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
import cn.shopex.ecshopx.merchant.repository.MerchantTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantTypeDeleteService {

	private final MerchantTypeRepository merchantTypeRepository;

	public MerchantTypeDeleteService(MerchantTypeRepository merchantTypeRepository) {
		this.merchantTypeRepository = merchantTypeRepository;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteMerchantType(long companyId, long typeId) {
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

}
