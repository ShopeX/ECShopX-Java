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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminSalespersonCreateService {

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final AdminSalespersonOperatorScopeHelper scopeHelper;

	public AdminSalespersonCreateService(ShopSalespersonMapper shopSalespersonMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			AdminSalespersonOperatorScopeHelper scopeHelper) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.scopeHelper = scopeHelper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void createSalesperson(long companyId, Map<String, Object> operatorJwt, String name, String mobile,
			String salespersonType, List<Long> shopIds, List<Long> distributorIds) {
		scopeHelper.assertShopIdsAllowed(operatorJwt, shopIds);
		scopeHelper.assertDistributorIdsAllowed(operatorJwt, distributorIds);
		if (!shopIds.isEmpty() && !distributorIds.isEmpty()) {
			throw new BadRequestException("不可同时选择门店和店铺");
		}
		if (!StringUtils.hasText(name) || !StringUtils.hasText(mobile)
				|| (shopIds.isEmpty() && distributorIds.isEmpty())) {
			throw new BadRequestException("请填写必填信息");
		}

		String encryptedMobile = sensitiveFieldEncryptor.encrypt(mobile.trim());
		ShopSalesperson existing = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getMobile, encryptedMobile)
				.eq(ShopSalesperson::getSalespersonType, salespersonType));
		if (existing != null) {
			if ("admin".equals(salespersonType)) {
				throw new ResourceException("当前手机号已经已绑定为管理员");
			}
			if ("verification_clerk".equals(salespersonType)) {
				throw new ResourceException("当前手机号已经已绑定为核销员");
			}
		}

		ShopSalesperson row = new ShopSalesperson();
		row.setCompanyId(companyId);
		row.setSalespersonType(salespersonType);
		row.setName(sensitiveFieldEncryptor.encrypt(name.trim()));
		row.setMobile(encryptedMobile);
		row.setCreatedTime(String.valueOf(Instant.now().getEpochSecond()));
		if (shopIds.isEmpty()) {
			row.setShopId("0");
		} else {
			row.setShopId(String.valueOf(shopIds.get(0)));
		}

		shopSalespersonMapper.insert(row);
		long salespersonId = row.getSalespersonId();

		for (Long shopId : shopIds) {
			ShopsRelSalesperson rel = new ShopsRelSalesperson();
			rel.setShopId(shopId);
			rel.setSalespersonId(salespersonId);
			rel.setCompanyId(companyId);
			rel.setStoreType("shop");
			shopsRelSalespersonMapper.insert(rel);
		}
		for (Long distributorId : distributorIds) {
			ShopsRelSalesperson rel = new ShopsRelSalesperson();
			rel.setShopId(distributorId);
			rel.setSalespersonId(salespersonId);
			rel.setCompanyId(companyId);
			rel.setStoreType("distributor");
			shopsRelSalespersonMapper.insert(rel);
		}
	}
}
