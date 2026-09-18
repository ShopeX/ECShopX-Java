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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class DistributorShoppingGuideUpdateService {

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final TransactionTemplate transactionTemplate;

	public DistributorShoppingGuideUpdateService(ShopSalespersonMapper shopSalespersonMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor, TransactionTemplate transactionTemplate) {
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.transactionTemplate = transactionTemplate;
	}

	public Map<String, Object> updateSalesman(long companyId, String salesmanId, String mobileTrimOrEmpty,
			String nameTrimOrEmpty, String isValidTrimOrEmpty, Long roleLongOrNull, List<Long> distributorIds) {
		String sid = salesmanId == null ? "" : salesmanId.trim();
		long id;
		try {
			id = Long.parseLong(sid);
		} catch (NumberFormatException e) {
			throw new ResourceException("更新的人员不存在");
		}

		ShopSalesperson current = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getSalespersonId, id)
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.last("LIMIT 1"));
		if (current == null) {
			throw new ResourceException("更新的人员不存在");
		}

		if (StringUtils.hasText(mobileTrimOrEmpty)) {
			String enc = sensitiveFieldEncryptor.encrypt(mobileTrimOrEmpty);
			String shopIdOfCurrent = current.getShopId() != null ? current.getShopId() : "0";
			// Same-store duplicate check: compare using the same encrypted mobile representation
			// as persisted in the database and as applied when creating shopping guides, so the
			// lookup matches existing stored mobile values.
			ShopSalesperson hit = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
					.eq(ShopSalesperson::getCompanyId, companyId)
					.eq(ShopSalesperson::getShopId, shopIdOfCurrent)
					.eq(ShopSalesperson::getMobile, enc)
					.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
					.last("LIMIT 1"));
			if (hit != null && !hit.getSalespersonId().equals(id)) {
				throw new ResourceException("当前手机号已经已绑定");
			}
		}

		final String encMobileForTx = StringUtils.hasText(mobileTrimOrEmpty)
				? sensitiveFieldEncryptor.encrypt(mobileTrimOrEmpty)
				: null;

		transactionTemplate.executeWithoutResult(status -> {
			long now = Instant.now().getEpochSecond();
			LambdaUpdateWrapper<ShopSalesperson> uw = new LambdaUpdateWrapper<ShopSalesperson>()
					.eq(ShopSalesperson::getSalespersonId, id)
					.eq(ShopSalesperson::getCompanyId, companyId)
					.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
					.set(ShopSalesperson::getSalespersonType, "shopping_guide")
					.set(ShopSalesperson::getUpdated, now);
			if (StringUtils.hasText(nameTrimOrEmpty)) {
				uw.set(ShopSalesperson::getName, sensitiveFieldEncryptor.encrypt(nameTrimOrEmpty));
			}
			if (encMobileForTx != null) {
				uw.set(ShopSalesperson::getMobile, encMobileForTx);
			}
			if (StringUtils.hasText(isValidTrimOrEmpty)) {
				uw.set(ShopSalesperson::getIsValid, isValidTrimOrEmpty.trim());
			}
			if (roleLongOrNull != null) {
				uw.set(ShopSalesperson::getRole, String.valueOf(roleLongOrNull));
			}
			int rows = shopSalespersonMapper.update(null, uw);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}

			if (distributorIds != null && !distributorIds.isEmpty()) {
				shopsRelSalespersonMapper.delete(new LambdaQueryWrapper<ShopsRelSalesperson>()
						.eq(ShopsRelSalesperson::getCompanyId, companyId)
						.eq(ShopsRelSalesperson::getSalespersonId, id));
				for (Long did : distributorIds) {
					if (did == null) {
						continue;
					}
					ShopsRelSalesperson rel = new ShopsRelSalesperson();
					rel.setShopId(did);
					rel.setSalespersonId(id);
					rel.setCompanyId(companyId);
					rel.setStoreType("distributor");
					shopsRelSalespersonMapper.insert(rel);
				}
			}
		});

		// Success envelope returns mobileFindData as empty object {} (not full entity).
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("status", true);
		payload.put("mobileFindData", new LinkedHashMap<>());
		return payload;
	}
}
