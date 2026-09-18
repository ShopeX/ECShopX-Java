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

import cn.shopex.ecshopx.merchant.mapper.MerchantDisabledDistributorIdsQueryMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MerchantDisabledDistributorIdsQueryService {

	private final MerchantDisabledDistributorIdsQueryMapper queryMapper;

	public MerchantDisabledDistributorIdsQueryService(MerchantDisabledDistributorIdsQueryMapper queryMapper) {
		this.queryMapper = queryMapper;
	}

	public List<Long> listDistributorIdsLinkedToDisabledMerchants(long companyId) {
		if (companyId <= 0L) {
			return List.of();
		}
		List<Long> rows = queryMapper.listDistributorIdsLinkedToDisabledMerchants(companyId);
		return rows == null ? List.of() : Collections.unmodifiableList(rows);
	}
}
