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

package cn.shopex.ecshopx.distribution.service.localdelivery;

import cn.shopex.ecshopx.common.port.localdelivery.LocalDeliveryDadaShopNoPort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidCoreQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LocalDeliveryDadaShopNoPortImpl implements LocalDeliveryDadaShopNoPort {

	private final DistributorMapper distributorMapper;
	private final DistributorIsValidCoreQueryService distributorIsValidCoreQueryService;

	public LocalDeliveryDadaShopNoPortImpl(
			DistributorMapper distributorMapper,
			DistributorIsValidCoreQueryService distributorIsValidCoreQueryService) {
		this.distributorMapper = distributorMapper;
		this.distributorIsValidCoreQueryService = distributorIsValidCoreQueryService;
	}

	@Override
	public String resolveShopNo(long companyId, long distributorId) {
		if (distributorId > 0) {
			Distributor d =
					distributorMapper.selectOne(
							new LambdaQueryWrapper<Distributor>()
									.eq(Distributor::getCompanyId, companyId)
									.eq(Distributor::getDistributorId, distributorId)
									.last("LIMIT 1"));
			if (d != null && d.getShopCode() != null) {
				return d.getShopCode();
			}
			return "";
		}
		Map<String, Object> self = distributorIsValidCoreQueryService.getDistributorSelf(companyId, true, Map.of());
		Object sc = self.get("shop_code");
		return sc == null ? "" : Objects.toString(sc, "").trim();
	}
}
