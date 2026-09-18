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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.domain.PromoterCashWithdrawal;
import cn.shopex.ecshopx.popularize.mapper.PromoterCashWithdrawalMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class PopularizePromoterCashWithdrawalApplyService {

	private final PromoterMapper promoterMapper;

	private final PopularizeSettingSaveService popularizeSettingSaveService;

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	private final PromoterCashWithdrawalMapper promoterCashWithdrawalMapper;

	private final PopularizePromoterCountWriteService popularizePromoterCountWriteService;

	private final TransactionTemplate transactionTemplate;

	public PopularizePromoterCashWithdrawalApplyService(
			PromoterMapper promoterMapper,
			PopularizeSettingSaveService popularizeSettingSaveService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			PromoterCashWithdrawalMapper promoterCashWithdrawalMapper,
			PopularizePromoterCountWriteService popularizePromoterCountWriteService,
			PlatformTransactionManager transactionManager) {
		this.promoterMapper = promoterMapper;
		this.popularizeSettingSaveService = popularizeSettingSaveService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.promoterCashWithdrawalMapper = promoterCashWithdrawalMapper;
		this.popularizePromoterCountWriteService = popularizePromoterCountWriteService;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public Map<String, Object> applyCashWithdrawal(
			HttpServletRequest request,
			Map<String, Object> claims,
			Map<String, Object> merged,
			String payTypeNorm,
			double moneyNum) {
		int moneyInt = (int) Math.floor(moneyNum);
		long companyId = resolveCompanyId(request, claims);
		String userIdStr = Objects.toString(claims.get("user_id"), "").trim();
		if (userIdStr.isEmpty()) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}
		long userId = parsePositiveLongOrZero(userIdStr);
		if (userId <= 0L) {
			throw new UnauthorizedException("还未授权，请授权手机号");
		}

		Promoter promoter =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getCompanyId, companyId)
								.eq(Promoter::getUserId, userId)
								.last("LIMIT 1"));
		if (promoter == null || promoter.getIsPromoter() == null || promoter.getIsPromoter() != 1) {
			throw new ResourceException("不是推广员，不可以申请");
		}
		if (promoter.getDisabled() != null && promoter.getDisabled() != 0) {
			throw new ResourceException("推广员已被商家禁用，请联系商家");
		}

		String payAccount;
		String accountName;
		if ("alipay".equals(payTypeNorm)) {
			LinkedHashMap<String, Object> promoterInfo = new LinkedHashMap<>();
			promoterInfo.put("alipay_account", promoter.getAlipayAccount());
			promoterInfo.put("alipay_name", promoter.getAlipayName());
			boolean alipayAccountIsset =
					promoterInfo.containsKey("alipay_account") && promoterInfo.get("alipay_account") != null;
			boolean alipayNameIsset =
					promoterInfo.containsKey("alipay_name") && promoterInfo.get("alipay_name") != null;
			if (!alipayAccountIsset || !alipayNameIsset) {
				throw new ResourceException("请先设置提现支付宝账号");
			}
			payAccount = String.valueOf(promoterInfo.get("alipay_account"));
			accountName = String.valueOf(promoterInfo.get("alipay_name"));
		} else {
			payAccount = Objects.toString(claims.get("open_id"), "").trim();
			accountName = Objects.toString(claims.get("username"), "").trim();
		}

		Map<String, Object> cfg = popularizeSettingSaveService.getConfig(companyId, null, null);
		Object rawLimit = cfg.get("limit_rebate");
		long minFen;
		String yuanPlain;
		if (rawLimit == null) {
			minFen = 100L;
			yuanPlain = "1";
		} else {
			try {
				BigDecimal yuan = new BigDecimal(String.valueOf(rawLimit).trim());
				if (yuan.compareTo(BigDecimal.ZERO) <= 0) {
					minFen = 100L;
					yuanPlain = "1";
				} else {
					long computedMinFen = yuan.multiply(BigDecimal.valueOf(100)).longValue();
					if (computedMinFen <= 0L) {
						minFen = 100L;
						yuanPlain = "1";
					} else {
						minFen = computedMinFen;
						yuanPlain = yuan.stripTrailingZeros().toPlainString();
					}
				}
			} catch (NumberFormatException | ArithmeticException e) {
				minFen = 100L;
				yuanPlain = "1";
			}
		}
		long moneyFenLong = (long) moneyInt;
		if (moneyFenLong < minFen) {
			throw new ResourceException("最少申请提现" + yuanPlain + "元");
		}

		PromoterCashWithdrawal entity = new PromoterCashWithdrawal();
		try {
			transactionTemplate.executeWithoutResult(
					status -> {
						entity.setCompanyId(Long.valueOf(companyId));
						entity.setUserId(String.valueOf(userId));
						entity.setPayType(payTypeNorm);
						entity.setPayAccount(payAccount);
						entity.setAccountName(accountName);
						entity.setMobile(
								sensitiveFieldEncryptor.encrypt(Objects.toString(claims.get("mobile"), "").trim()));
						entity.setWxaAppid(Objects.toString(claims.get("wxapp_appid"), "").trim());
						entity.setMoney(moneyInt);
						entity.setStatus("apply");
						entity.setRemarks(null);
						entity.setCreated((int) (System.currentTimeMillis() / 1000L));
						entity.setUpdated((int) (System.currentTimeMillis() / 1000L));
						promoterCashWithdrawalMapper.insert(entity);
						popularizePromoterCountWriteService.applyCashWithdrawal(companyId, userId, moneyInt);
					});
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			String msg = e.getMessage();
			throw new ResourceException(msg == null ? "系统错误" : msg);
		}

		return toSnakeCaseRow(entity);
	}

	private static long resolveCompanyId(HttpServletRequest request, Map<String, Object> claims) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long fromAttr = parsePositiveLongOrZero(companyAttr);
		if (fromAttr > 0L) {
			return fromAttr;
		}
		long fromClaims = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (fromClaims > 0L) {
			return fromClaims;
		}
		throw new UnauthorizedException("Unable to authenticate user.");
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, Object> toSnakeCaseRow(PromoterCashWithdrawal e) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", e.getId());
		row.put("company_id", e.getCompanyId());
		row.put("user_id", e.getUserId());
		row.put("account_name", e.getAccountName());
		row.put("pay_account", e.getPayAccount());
		row.put("mobile", e.getMobile());
		row.put("money", e.getMoney());
		row.put("status", e.getStatus());
		row.put("remarks", e.getRemarks());
		row.put("pay_type", e.getPayType());
		row.put("wxa_appid", e.getWxaAppid());
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());
		return row;
	}
}
