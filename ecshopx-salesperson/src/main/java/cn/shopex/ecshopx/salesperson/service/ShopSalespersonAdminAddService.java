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
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.service.PromoterShopStatusSalespersonService;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class ShopSalespersonAdminAddService {

	private static final Logger log = LoggerFactory.getLogger(ShopSalespersonAdminAddService.class);

	private final MemberAccountService memberAccountService;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final PromoterShopStatusSalespersonService promoterShopStatusSalespersonService;
	private final TransactionTemplate transactionTemplate;

	public ShopSalespersonAdminAddService(MemberAccountService memberAccountService,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor, ShopSalespersonMapper shopSalespersonMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			PromoterShopStatusSalespersonService promoterShopStatusSalespersonService,
			TransactionTemplate transactionTemplate) {
		this.memberAccountService = memberAccountService;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.promoterShopStatusSalespersonService = promoterShopStatusSalespersonService;
		this.transactionTemplate = transactionTemplate;
	}

	public Map<String, Object> addsalesperson(long companyId, Map<String, Object> mergedParams) {
		String mobileTrim = stringOf(mergedParams.get("mobile"));
		if (!StringUtils.hasText(mobileTrim)) {
			throw new BadRequestException("请填写手机号", Map.of("mobile", List.of("请填写手机号")));
		}

		String nameTrim = stringOf(mergedParams.get("name"));
		if (!StringUtils.hasText(nameTrim)) {
			throw new BadRequestException("请填写导购员姓名", Map.of("name", List.of("请填写导购员姓名")));
		}

		long requestDistributorId = parseNonNegativeDistributorId(mergedParams.get("distributor_id"));
		String distributorIdStr = String.valueOf(requestDistributorId);

		String isValidNormalized = normalizeIsValid(mergedParams);

		Members m = memberAccountService.findMemberByCompanyAndMobile(companyId, mobileTrim);
		if (m == null || m.getUserId() == null) {
			throw new ResourceException("当前手机号还不是会员");
		}
		int memberUserIdInt = m.getUserId().intValue();
		long memberUserIdLong = m.getUserId().longValue();

		Map<String, Object> dist = distributorRepositoryGetInfoSimpleService.resolveDistributorForAddSalesman(
				companyId, new String[] { distributorIdStr });
		if (log.isDebugEnabled()) {
			log.debug("addsalesperson mergedParams={}, dist={}", mergedParams, dist);
		}
		if (dist == null || dist.isEmpty()) {
			throw new ResourceException("店铺未开启业务员配置！");
		}
		Object didObj = dist.get("distributor_id");
		if (!(didObj instanceof Number)) {
			throw new ResourceException("店铺未开启业务员配置！");
		}
		if (!Boolean.TRUE.equals(dist.get("is_open_salesman"))) {
			throw new ResourceException("店铺未开启业务员配置！");
		}
		long resolvedShopId = ((Number) didObj).longValue();
		String shopIdStr = String.valueOf(resolvedShopId);

		LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
		dataMap.put("mobile", mobileTrim);
		dataMap.put("name", nameTrim);
		dataMap.put("role", "");
		dataMap.put("distributor_id", 0);
		dataMap.put("company_id", companyId);
		dataMap.put("salesperson_type", "shopping_guide");
		dataMap.put("is_valid", isValidNormalized);
		dataMap.put("number", "");
		dataMap.put("employee_status", 1);
		dataMap.put("user_id", memberUserIdLong);
		dataMap.put("shop_id", requestDistributorId);

		ShopSalesperson dupUser = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getUserId, memberUserIdInt)
				.eq(ShopSalesperson::getShopId, shopIdStr)
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.last("LIMIT 1"));
		if (dupUser != null) {
			throw new ResourceException("当前会员已添加！");
		}

		String encMobile = sensitiveFieldEncryptor.encrypt(mobileTrim);
		ShopSalesperson dupMobile = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getUserId, memberUserIdInt)
				.eq(ShopSalesperson::getMobile, encMobile)
				.eq(ShopSalesperson::getShopId, shopIdStr)
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.last("LIMIT 1"));
		if (dupMobile != null) {
			throw new ResourceException("当前手机号已绑定");
		}

		long epochSecond = Instant.now().getEpochSecond();
		ShopSalesperson row = new ShopSalesperson();
		row.setName(sensitiveFieldEncryptor.encrypt(nameTrim));
		row.setMobile(encMobile);
		row.setCompanyId(companyId);
		row.setUserId(memberUserIdInt);
		row.setSalespersonType("shopping_guide");
		row.setNumber("");
		row.setRole("");
		row.setEmployeeStatus(1);
		row.setIsValid(isValidNormalized);
		row.setShopId(shopIdStr);
		row.setCreatedTime(String.valueOf(epochSecond));
		row.setCreated(epochSecond);
		row.setUpdated(epochSecond);

		transactionTemplate.executeWithoutResult(status -> {
			try {
				shopSalespersonMapper.insert(row);
				if (!"0".equals(shopIdStr) && resolvedShopId > 0L) {
					ShopsRelSalesperson rel = new ShopsRelSalesperson();
					rel.setShopId(resolvedShopId);
					rel.setSalespersonId(row.getSalespersonId());
					rel.setCompanyId(companyId);
					rel.setStoreType("shop");
					shopsRelSalespersonMapper.insert(rel);
				}
			} catch (DataIntegrityViolationException e) {
				if (isDuplicateKeyConstraint(e)) {
					throw new ResourceException("当前手机号已绑定");
				}
				throw e;
			}
		});

		promoterShopStatusSalespersonService.updateShopStatusSalesperson(companyId, memberUserIdLong);

		LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();
		envelope.put("status", 1);
		envelope.put("code", 0);
		envelope.put("data", dataMap);
		envelope.put("inoutData", new LinkedHashMap<>(mergedParams));
		return envelope;
	}

	private static boolean isDuplicateKeyConstraint(DataIntegrityViolationException e) {
		if (e instanceof DuplicateKeyException) {
			return true;
		}
		Throwable t = e.getMostSpecificCause();
		if (t != null) {
			String cn = t.getClass().getName();
			if (cn.contains("SQLIntegrityConstraintViolationException")) {
				String msg = t.getMessage();
				return msg != null && msg.toLowerCase().contains("duplicate");
			}
		}
		String msg = e.getMessage();
		return msg != null && (msg.contains("Duplicate") || msg.toLowerCase().contains("duplicate entry"));
	}

	private static long parseNonNegativeDistributorId(Object raw) {
		if (raw == null) {
			throw new BadRequestException("请选择导购员所属店铺",
					Map.of("distributor_id", List.of("请选择导购员所属店铺")));
		}
		long v;
		if (raw instanceof Number n) {
			v = n.longValue();
		} else {
			String s = String.valueOf(raw).trim();
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException("请选择导购员所属店铺",
						Map.of("distributor_id", List.of("请选择导购员所属店铺")));
			}
			try {
				v = Long.parseLong(s);
			} catch (NumberFormatException e) {
				throw new BadRequestException("请选择导购员所属店铺",
						Map.of("distributor_id", List.of("请选择导购员所属店铺")));
			}
		}
		if (v < 0L) {
			throw new BadRequestException("请选择导购员所属店铺",
					Map.of("distributor_id", List.of("请选择导购员所属店铺")));
		}
		return v;
	}

	private static String normalizeIsValid(Map<String, Object> mergedParams) {
		if (!mergedParams.containsKey("is_valid")) {
			return "false";
		}
		Object v = mergedParams.get("is_valid");
		String normalized;
		if (v instanceof Boolean b) {
			normalized = b ? "true" : "false";
		} else {
			normalized = String.valueOf(v).trim();
		}
		if ("true".equals(normalized) || "false".equals(normalized)) {
			return normalized;
		}
		throw new BadRequestException("请选择是否开启", Map.of("is_valid", List.of("请选择是否开启")));
	}

	private static String stringOf(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}
}
