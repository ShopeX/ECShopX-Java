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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.BasicConfig;
import cn.shopex.ecshopx.distribution.domain.CashWithdrawal;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.CashWithdrawalMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.repository.BasicConfigWriteRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class CashWithdrawalApplyService {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final DistributorMapper distributorMapper;
	private final ObjectMapper objectMapper;
	private final BasicConfigWriteRepository basicConfigWriteRepository;
	private final CashWithdrawalMapper cashWithdrawalMapper;
	private final DistributeCountWriteService distributeCountWriteService;
	private final TransactionTemplate transactionTemplate;
	private final MessageSource messageSource;

	public CashWithdrawalApplyService(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			DistributorMapper distributorMapper,
			ObjectMapper objectMapper,
			BasicConfigWriteRepository basicConfigWriteRepository,
			CashWithdrawalMapper cashWithdrawalMapper,
			DistributeCountWriteService distributeCountWriteService,
			PlatformTransactionManager transactionManager,
			MessageSource messageSource) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.distributorMapper = distributorMapper;
		this.objectMapper = objectMapper;
		this.basicConfigWriteRepository = basicConfigWriteRepository;
		this.cashWithdrawalMapper = cashWithdrawalMapper;
		this.distributeCountWriteService = distributeCountWriteService;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.messageSource = messageSource;
	}

	public Map<String, Object> applyCashWithdrawal(
			long companyId, Map<String, Object> authClaims, Object moneyRaw, Locale locale) {
		long moneyLong = parseMoneyToLong(moneyRaw);
		long moneyFloored = (long) Math.floor(moneyLong);
		if (moneyFloored > Integer.MAX_VALUE || moneyFloored < Integer.MIN_VALUE) {
			throw new BadRequestException("参数错误");
		}

		if (moneyFloored < 100L) {
			throw new ResourceException(
					messageSource.getMessage(
							"distribution.cash_withdrawal.withdrawal_min_amount",
							null,
							"提现金额不能少于1元",
							locale));
		}
		if (moneyFloored > 80000L) {
			throw new ResourceException(
					messageSource.getMessage(
							"distribution.cash_withdrawal.withdrawal_max_amount",
							null,
							"单次提现金额不能超过800元",
							locale));
		}

		Map<String, Object> distributorInfo =
				distributorRepositoryGetInfoSimpleService.getDistributorInfoForCompanyShop(companyId, 0L);
		if (cashWithdrawalRequiresMainButMainMissing(companyId)) {
			throw new ResourceException(
					messageSource.getMessage(
							"distribution.cash_withdrawal.distributor_invalid",
							null,
							"店铺无效",
							locale));
		}
		if (distributorInfo == null || distributorInfo.isEmpty()) {
			throw new ResourceException(
					messageSource.getMessage(
							"distribution.cash_withdrawal.distributor_invalid",
							null,
							"店铺无效",
							locale));
		}

		BasicConfig cfg = basicConfigWriteRepository.getInfoByCompanyId(companyId);
		if (cfg != null && StringUtils.hasText(cfg.getLimitRebate())) {
			Long limitFen = parseLimitRebateFenSkippable(cfg.getLimitRebate());
			if (limitFen != null && moneyFloored < limitFen) {
				BigDecimal yuan =
						BigDecimal.valueOf(limitFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
				throw new ResourceException(
						String.format("最低提现金额为%s元", yuan.toPlainString()));
			}
		}

		int moneyInt = (int) moneyFloored;
		CashWithdrawal entity = new CashWithdrawal();
		try {
			transactionTemplate.executeWithoutResult(
					status -> {
						entity.setCompanyId(companyId);
						entity.setOpenId(Objects.toString(authClaims.get("open_id"), ""));
						entity.setUserId(Objects.toString(authClaims.get("user_id"), ""));
						entity.setWxaAppid(Objects.toString(authClaims.get("wxapp_appid"), ""));
						entity.setMoney(moneyInt);
						entity.setStatus("apply");
						entity.setDistributorId(
								String.valueOf(distributorInfo.getOrDefault("distributor_id", 0L)));
						entity.setDistributorName(Objects.toString(distributorInfo.get("name"), ""));
						entity.setDistributorMobile(Objects.toString(distributorInfo.get("mobile"), ""));
						entity.setShopId(toLongOrZero(distributorInfo.get("shop_id")));
						entity.setRemarks(null);
						long now = System.currentTimeMillis() / 1000L;
						entity.setCreated(now);
						entity.setUpdated(now);
						cashWithdrawalMapper.insert(entity);
						distributeCountWriteService.applyCashWithdrawal(
								companyId, entity.getDistributorId(), moneyInt);
					});
		} catch (ResourceException e) {
			throw e;
		} catch (BadRequestException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ResourceException("系统错误，请稍后再试");
		}

		return buildRowMap(entity);
	}

	private static Map<String, Object> buildRowMap(CashWithdrawal entity) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", entity.getId());
		row.put("company_id", entity.getCompanyId());
		row.put("distributor_id", entity.getDistributorId());
		row.put("shop_id", entity.getShopId());
		row.put("distributor_name", entity.getDistributorName());
		row.put("open_id", entity.getOpenId());
		row.put("user_id", entity.getUserId());
		row.put("distributor_mobile", entity.getDistributorMobile());
		row.put("money", entity.getMoney());
		row.put("status", entity.getStatus());
		row.put("remarks", entity.getRemarks());
		row.put("wxa_appid", entity.getWxaAppid());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		return row;
	}

	private static long toLongOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long parseLimitRebateFenSkippable(String raw) {
		String t = raw.trim();
		if (t.isEmpty()) {
			return null;
		}
		if (!t.matches("^[+-]?\\d+$")) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long parseMoneyToLong(Object moneyRaw) {
		if (moneyRaw == null) {
			return 0L;
		}
		if (moneyRaw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(moneyRaw).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		if (!s.matches("^[+-]?\\d+$")) {
			throw new BadRequestException("参数错误");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}
	}

	/**
	 * 与 {@link DistributorRepositoryGetInfoSimpleService#getDistributorInfoForCompanyShop} 中「默认店需切总店」分支
	 * 该场景下佣金提现要求总店存在，否则「店铺无效」；不依赖共享 Service 清空 Map 的副作用。
	 */
	private boolean cashWithdrawalRequiresMainButMainMissing(long companyId) {
		Distributor defaultEntity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getIsDefault, 1)
				.last("LIMIT 1"));
		if (defaultEntity == null) {
			return false;
		}
		Map<String, Object> defaultRow = DistributorRowMaps.toApiRow(defaultEntity, objectMapper);
		if (!shouldUseMainDistributorInsteadOfDefaultRow(defaultRow)) {
			return false;
		}
		Distributor mainEntity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getDistributorSelf, 1)
				.last("LIMIT 1"));
		return mainEntity == null;
	}

	private static boolean shouldUseMainDistributorInsteadOfDefaultRow(Map<String, Object> distributorInfo) {
		Object isValid = distributorInfo.get("is_valid");
		if ("false".equals(isValid) || Boolean.FALSE.equals(isValid)) {
			return true;
		}
		Object self = distributorInfo.get("distributor_self");
		return Integer.valueOf(0).equals(self) || Long.valueOf(0L).equals(self) || "0".equals(self);
	}
}
