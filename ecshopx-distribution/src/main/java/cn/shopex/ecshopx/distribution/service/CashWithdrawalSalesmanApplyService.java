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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.distribution.domain.BasicConfig;
import cn.shopex.ecshopx.distribution.domain.CashWithdrawal;
import cn.shopex.ecshopx.distribution.mapper.CashWithdrawalMapper;
import cn.shopex.ecshopx.distribution.repository.BasicConfigWriteRepository;
import cn.shopex.ecshopx.popularize.service.SalesmanBrokerageCountListQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class CashWithdrawalSalesmanApplyService {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final BasicConfigWriteRepository basicConfigWriteRepository;
	private final SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService;
	private final CashWithdrawalMapper cashWithdrawalMapper;
	private final TransactionTemplate transactionTemplate;
	private final MessageSource messageSource;

	public CashWithdrawalSalesmanApplyService(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			BasicConfigWriteRepository basicConfigWriteRepository,
			SalesmanBrokerageCountListQueryService salesmanBrokerageCountListQueryService,
			CashWithdrawalMapper cashWithdrawalMapper,
			PlatformTransactionManager transactionManager,
			MessageSource messageSource) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.basicConfigWriteRepository = basicConfigWriteRepository;
		this.salesmanBrokerageCountListQueryService = salesmanBrokerageCountListQueryService;
		this.cashWithdrawalMapper = cashWithdrawalMapper;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.messageSource = messageSource;
	}

	public Map<String, Object> salesmanApplyCashWithdrawal(
			long companyId,
			long distributorId,
			Map<String, Object> authClaims,
			Object moneyRaw,
			Locale locale) {
		String userId = Objects.toString(authClaims.get("user_id"), "").trim();
		if (!StringUtils.hasText(userId)) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}

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

		Map<String, Object> distributorInfo = distributorRepositoryGetInfoSimpleService
				.getDistributorApiRowByCompanyAndDistributorId(companyId, distributorId)
				.orElse(null);
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

		Map<String, Object> brokerageParams = new LinkedHashMap<>();
		brokerageParams.put("user_id", userId);
		brokerageParams.put("distributor_id", String.valueOf(distributorId));
		Map<String, Object> agg = salesmanBrokerageCountListQueryService.getSalesmanBrokerageCountAggregate(brokerageParams);
		long rebateSumClose = toLongMoney(agg.get("rebate_sum_close"));

		LambdaQueryWrapper<CashWithdrawal> w = new LambdaQueryWrapper<CashWithdrawal>()
				.eq(CashWithdrawal::getCompanyId, companyId)
				.eq(CashWithdrawal::getDistributorId, String.valueOf(distributorId))
				.eq(CashWithdrawal::getUserId, userId)
				.in(CashWithdrawal::getStatus, Arrays.asList("apply", "process", "success"));
		List<CashWithdrawal> list = cashWithdrawalMapper.selectList(w);
		long sumWithdrawal =
				list.stream().mapToLong(e -> e.getMoney() == null ? 0L : e.getMoney().longValue()).sum();

		if (rebateSumClose < sumWithdrawal + moneyFloored) {
			long moneyCanWithdrawal = rebateSumClose - sumWithdrawal;
			BigDecimal yuan =
					BigDecimal.valueOf(moneyCanWithdrawal).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
			throw new ResourceException(
					messageSource.getMessage(
							"distribution.cash_withdrawal.withdrawal_amount_exceeded",
							new Object[] {yuan.toPlainString()},
							"可提现余额不足",
							locale));
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
					});
		} catch (ResourceException e) {
			throw e;
		} catch (BadRequestException e) {
			throw e;
		} catch (RuntimeException e) {
			String msg = e.getMessage();
			throw new ResourceException(
					msg != null && !msg.isBlank() ? msg : "系统错误，请稍后再试");
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

	private static long toLongMoney(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
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
}
