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

package cn.shopex.ecshopx.orders.service.localdelivery;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.localdelivery.ShansongCompanyCredentialsPort;
import cn.shopex.ecshopx.common.port.localdelivery.ShansongOpenCredentials;
import cn.shopex.ecshopx.orders.domain.CompanyRelShansong;
import cn.shopex.ecshopx.orders.mapper.CompanyRelShansongMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShansongCompanyCredentialsPortImpl implements ShansongCompanyCredentialsPort {

	private final CompanyRelShansongMapper companyRelShansongMapper;

	public ShansongCompanyCredentialsPortImpl(CompanyRelShansongMapper companyRelShansongMapper) {
		this.companyRelShansongMapper = companyRelShansongMapper;
	}

	@Override
	public ShansongOpenCredentials loadForCompany(long companyId) {
		CompanyRelShansong row =
				companyRelShansongMapper.selectOne(
						new LambdaQueryWrapper<CompanyRelShansong>()
								.eq(CompanyRelShansong::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (row == null) {
			throw new ResourceException("请先配置闪送应用信息");
		}
		String shopId = row.getShopId() == null ? "" : row.getShopId().trim();
		String clientId = row.getClientId() == null ? "" : row.getClientId().trim();
		String appSecret = row.getAppSecret() == null ? "" : row.getAppSecret().trim();
		if (!StringUtils.hasText(shopId) || !StringUtils.hasText(clientId) || !StringUtils.hasText(appSecret)) {
			throw new ResourceException("请先配置闪送应用信息");
		}
		boolean online = Boolean.TRUE.equals(row.getOnline());
		return new ShansongOpenCredentials(shopId, clientId, appSecret, online);
	}
}
