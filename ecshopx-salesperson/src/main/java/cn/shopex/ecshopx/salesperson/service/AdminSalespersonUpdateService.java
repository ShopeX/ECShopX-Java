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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminSalespersonUpdateService {

	private final AdminSalespersonOperatorScopeHelper scopeHelper;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public AdminSalespersonUpdateService(AdminSalespersonOperatorScopeHelper scopeHelper,
			ShopSalespersonMapper shopSalespersonMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.scopeHelper = scopeHelper;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateSalesperson(long companyId, Map<String, Object> operatorJwt, long salespersonId, String name,
			String mobile, String salespersonType, List<Long> shopIds, List<Long> distributorIds) {
		scopeHelper.assertShopIdsAllowed(operatorJwt, shopIds);
		scopeHelper.assertDistributorIdsAllowed(operatorJwt, distributorIds);
		if (!shopIds.isEmpty() && !distributorIds.isEmpty()) {
			throw new BadRequestException("不可同时选择门店和店铺");
		}
		if (!StringUtils.hasText(name) || !StringUtils.hasText(mobile)
				|| (shopIds.isEmpty() && distributorIds.isEmpty())) {
			throw new BadRequestException("请填写必填信息");
		}

		ShopSalesperson current = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.eq(ShopSalesperson::getSalespersonType, salespersonType)
				.last("LIMIT 1"));
		if (current == null) {
			throw new ResourceException("更新的人员不存在");
		}

		String encMobile = sensitiveFieldEncryptor.encrypt(mobile.trim());
		ShopSalesperson byMobile = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getMobile, encMobile)
				.eq(ShopSalesperson::getSalespersonType, salespersonType)
				.last("LIMIT 1"));
		if (byMobile != null && !byMobile.getSalespersonId().equals(salespersonId)) {
			if ("admin".equals(salespersonType)) {
				throw new ResourceException("当前手机号已经已绑定为管理员");
			}
			if ("verification_clerk".equals(salespersonType)) {
				throw new ResourceException("当前手机号已经已绑定为核销员");
			}
		}

		long now = Instant.now().getEpochSecond();
		LambdaUpdateWrapper<ShopSalesperson> uw = new LambdaUpdateWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getSalespersonId, salespersonId)
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getSalespersonType, salespersonType)
				.set(ShopSalesperson::getName, sensitiveFieldEncryptor.encrypt(name.trim()))
				.set(ShopSalesperson::getMobile, encMobile)
				.set(ShopSalesperson::getSalespersonType, salespersonType)
				.set(ShopSalesperson::getUpdated, now);
		int rows = shopSalespersonMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		if (!shopIds.isEmpty()) {
			shopsRelSalespersonMapper.delete(new LambdaQueryWrapper<ShopsRelSalesperson>()
					.eq(ShopsRelSalesperson::getCompanyId, companyId)
					.eq(ShopsRelSalesperson::getSalespersonId, salespersonId));
			for (Long sid : shopIds) {
				if (sid == null) {
					continue;
				}
				ShopsRelSalesperson rel = new ShopsRelSalesperson();
				rel.setShopId(sid);
				rel.setSalespersonId(salespersonId);
				rel.setCompanyId(companyId);
				rel.setStoreType("shop");
				shopsRelSalespersonMapper.insert(rel);
			}
		}
		if (!distributorIds.isEmpty()) {
			shopsRelSalespersonMapper.delete(new LambdaQueryWrapper<ShopsRelSalesperson>()
					.eq(ShopsRelSalesperson::getCompanyId, companyId)
					.eq(ShopsRelSalesperson::getSalespersonId, salespersonId));
			for (Long distributorId : distributorIds) {
				if (distributorId == null) {
					continue;
				}
				ShopsRelSalesperson rel = new ShopsRelSalesperson();
				rel.setShopId(distributorId);
				rel.setSalespersonId(salespersonId);
				rel.setCompanyId(companyId);
				rel.setStoreType("distributor");
				shopsRelSalespersonMapper.insert(rel);
			}
		}
	}
}
