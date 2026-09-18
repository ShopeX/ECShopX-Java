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

import cn.shopex.ecshopx.common.distribution.DistributorWhiteListCheckUserValidPort;
import org.springframework.stereotype.Service;

@Service
public class DistributorWhiteListCheckUserValidPortImpl implements DistributorWhiteListCheckUserValidPort {

	private final DistributorWhiteListCheckUserValidService delegate;

	public DistributorWhiteListCheckUserValidPortImpl(DistributorWhiteListCheckUserValidService delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean checkUserValidInDistributor(long distributorId, long userId, long companyId) {
		return delegate.checkUserValidInDistributor(distributorId, userId, companyId);
	}

	@Override
	public boolean checkUserValidCommon(long distributorId, long userId, long companyId) {
		return delegate.checkUserValidCommon(distributorId, userId, companyId);
	}
}
