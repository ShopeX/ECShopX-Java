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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayWithdrawSet;
import cn.shopex.ecshopx.adapay.mapper.AdapayWithdrawSetMapper;
import cn.shopex.ecshopx.adapay.util.AdapayWithdrawRuleLegacySerializeUtil;
import cn.shopex.ecshopx.common.adapay.AdapayOperationLogRecordPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapayWithdrawSetSaveService {

	private static final List<String> WITHDRAW_INDEX_CASH_TYPES = List.of("T0", "T1", "D1");

	private static final Pattern DISTRIBUTOR_MONEY_PATTERN = Pattern.compile(
			"^(([0-9]+.[0-9]*[1-9][0-9]*)|([0-9]*[1-9][0-9]*.[0-9]+)|([0-9]*[1-9][0-9]*))|0?.0+|0$");

	private final AdapayWithdrawSetMapper adapayWithdrawSetMapper;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final AdapayOperationLogRecordPort adapayOperationLogRecordPort;

	public AdapayWithdrawSetSaveService(
			AdapayWithdrawSetMapper adapayWithdrawSetMapper,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			AdapayOperationLogRecordPort adapayOperationLogRecordPort) {
		this.adapayWithdrawSetMapper = adapayWithdrawSetMapper;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.adapayOperationLogRecordPort = adapayOperationLogRecordPort;
	}

	public Map<String, Object> index(long companyId, long distributorId) {
		LambdaQueryWrapper<AdapayWithdrawSet> w = new LambdaQueryWrapper<AdapayWithdrawSet>()
				.eq(AdapayWithdrawSet::getCompanyId, companyId)
				.eq(AdapayWithdrawSet::getDistributorId, distributorId)
				.last("LIMIT 1");
		AdapayWithdrawSet row = adapayWithdrawSetMapper.selectOne(w);
		if (row == null) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("cash_types", WITHDRAW_INDEX_CASH_TYPES);
			return empty;
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", row.getId());
		out.put("isAuto", row.getIsAuto());
		out.put("company_id", row.getCompanyId());
		out.put("distributor_id", row.getDistributorId());
		out.put("cash_type", row.getCashType() != null ? row.getCashType() : "");
		String fenStr = row.getCashAmt();
		if (!StringUtils.hasText(fenStr)) {
			fenStr = "0";
		} else {
			fenStr = fenStr.trim();
		}
		String cashAmtYuan = new BigDecimal(fenStr)
				.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
				.toPlainString();
		out.put("cash_amt", cashAmtYuan);
		String col = row.getRule();
		if (!StringUtils.hasText(col)) {
			out.put("rule", new LinkedHashMap<String, Object>());
		} else {
			out.put("rule", AdapayWithdrawRuleLegacySerializeUtil.deserializeWithdrawRule(col));
		}
		out.put("cash_types", WITHDRAW_INDEX_CASH_TYPES);
		return out;
	}

	public void save(long companyId, long jwtDistributorId, long jwtOperatorId, Map<String, Object> input) {
		long requestDistributorId = resolveRequestDistributorId(input, jwtDistributorId);
		boolean isAuto = parseIsAuto(input.get("isAuto"));
		Map<String, Object> ruleMap = extractRuleMap(input);

		if (isAuto) {
			String typeStr = stringify(ruleMap.get("type")).trim();
			if (!StringUtils.hasText(typeStr)) {
				throw new ResourceException("规则类型必填");
			}
			validateCashAmtWhenAutoWithdraw(input);
			validateCashTypeWhenAutoWithdraw(input);
		}

		LambdaQueryWrapper<AdapayWithdrawSet> w = new LambdaQueryWrapper<AdapayWithdrawSet>()
				.eq(AdapayWithdrawSet::getCompanyId, companyId)
				.eq(AdapayWithdrawSet::getDistributorId, requestDistributorId)
				.last("LIMIT 1");
		AdapayWithdrawSet existingRow = adapayWithdrawSetMapper.selectOne(w);
		Long existingId = existingRow != null ? existingRow.getId() : null;

		String cashAmtFen;
		String cashTypeVal;

		if (isAuto) {
			String yuan = extractCashAmtYuanString(input.get("cash_amt"));
			cashAmtFen = checkCashAmtYuanToFen(yuan);
			cashTypeVal = String.valueOf(input.get("cash_type")).trim();
		} else {
			if (hasNonEmptyCashAmt(input)) {
				String yuan = extractCashAmtYuanString(input.get("cash_amt"));
				cashAmtFen = checkCashAmtYuanToFen(yuan);
			} else if (existingRow != null) {
				cashAmtFen = existingRow.getCashAmt() != null ? existingRow.getCashAmt() : "0";
			} else {
				cashAmtFen = "0";
			}
			if (hasNonEmptyCashType(input)) {
				cashTypeVal = String.valueOf(input.get("cash_type")).trim();
			} else if (existingRow != null) {
				cashTypeVal = existingRow.getCashType() != null ? existingRow.getCashType() : "";
			} else {
				cashTypeVal = "";
			}
		}

		String ruleSerialized = AdapayWithdrawRuleLegacySerializeUtil.serialize(ruleMap);

		if (existingId != null) {
			AdapayWithdrawSet row = adapayWithdrawSetMapper.selectById(existingId);
			if (row == null) {
				throw new ResourceException("未查询到更新数据");
			}
			row.setCompanyId(companyId);
			row.setDistributorId(requestDistributorId);
			row.setIsAuto(isAuto);
			row.setCashAmt(cashAmtFen);
			row.setCashType(cashTypeVal);
			row.setRule(ruleSerialized);
			adapayWithdrawSetMapper.updateById(row);
		} else {
			AdapayWithdrawSet entity = new AdapayWithdrawSet();
			entity.setCompanyId(companyId);
			entity.setDistributorId(requestDistributorId);
			entity.setIsAuto(isAuto);
			entity.setCashAmt(cashAmtFen);
			entity.setCashType(cashTypeVal);
			entity.setRule(ruleSerialized);
			adapayWithdrawSetMapper.insert(entity);
		}

		Map<String, Object> dist =
				distributorRepositoryGetInfoSimpleService.getDistributorInfoForCompanyShop(companyId, requestDistributorId);
		String name = "";
		if (dist != null && !dist.isEmpty()) {
			Object n = dist.get("name");
			name = n != null ? n.toString() : "";
		}

		Map<String, Object> logParams = new HashMap<>();
		logParams.put("company_id", companyId);
		logParams.put("name", name);
		adapayOperationLogRecordPort.logRecord(logParams, jwtOperatorId, "withdrawset", "merchant", jwtOperatorId);
	}

	private static long resolveRequestDistributorId(Map<String, Object> input, long jwtDistributorId) {
		Object raw = input.get("distributor_id");
		if (raw == null) {
			return jwtDistributorId;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : jwtDistributorId;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return jwtDistributorId;
			}
			try {
				long v = Long.parseLong(t);
				return v > 0L ? v : jwtDistributorId;
			} catch (NumberFormatException e) {
				throw new ResourceException("请选择店铺");
			}
		}
		String t = String.valueOf(raw).trim();
		if (!StringUtils.hasText(t)) {
			return jwtDistributorId;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0L ? v : jwtDistributorId;
		} catch (NumberFormatException e) {
			throw new ResourceException("请选择店铺");
		}
	}

	private static boolean parseIsAuto(Object raw) {
		if (raw == null) {
			throw new ResourceException("是否开启自动提现");
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		String s = String.valueOf(raw).trim();
		if ("true".equalsIgnoreCase(s)) {
			return true;
		}
		if ("false".equalsIgnoreCase(s)) {
			return false;
		}
		throw new ResourceException("是否开启自动提现");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> extractRuleMap(Map<String, Object> input) {
		Object r = input.get("rule");
		if (r instanceof Map<?, ?> m) {
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
			return out;
		}
		return new LinkedHashMap<>();
	}

	private static void validateCashAmtWhenAutoWithdraw(Map<String, Object> input) {
		if (!input.containsKey("cash_amt")) {
			throw new ResourceException("店铺账号提现金额");
		}
		Object raw = input.get("cash_amt");
		if (raw == null) {
			throw new ResourceException("店铺账号提现金额");
		}
		if (raw instanceof Boolean || raw instanceof Map<?, ?> || raw instanceof Iterable<?>
				|| raw.getClass().isArray()) {
			throw new ResourceException("店铺账号提现金额");
		}
		String trimmed;
		if (raw instanceof String str) {
			trimmed = str.trim();
			if (!StringUtils.hasText(trimmed)) {
				throw new ResourceException("店铺账号提现金额");
			}
		} else if (raw instanceof BigDecimal bd) {
			trimmed = bd.stripTrailingZeros().toPlainString();
		} else if (raw instanceof Number) {
			trimmed = new BigDecimal(raw.toString()).stripTrailingZeros().toPlainString();
		} else {
			throw new ResourceException("店铺账号提现金额");
		}
		BigDecimal bd;
		try {
			bd = new BigDecimal(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("店铺账号提现金额");
		}
		if (bd.compareTo(BigDecimal.ZERO) < 0) {
			throw new ResourceException("店铺账号提现金额");
		}
	}

	private static void validateCashTypeWhenAutoWithdraw(Map<String, Object> input) {
		if (!input.containsKey("cash_type")) {
			throw new ResourceException("提现类型");
		}
		Object raw = input.get("cash_type");
		if (raw == null) {
			throw new ResourceException("提现类型");
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("提现类型");
		}
	}

	private static boolean hasNonEmptyCashAmt(Map<String, Object> input) {
		if (!input.containsKey("cash_amt")) {
			return false;
		}
		Object raw = input.get("cash_amt");
		if (raw == null) {
			return false;
		}
		if (raw instanceof String s) {
			return StringUtils.hasText(s.trim());
		}
		if (raw instanceof Boolean || raw instanceof Map<?, ?> || raw instanceof Iterable<?>
				|| raw.getClass().isArray()) {
			return true;
		}
		return true;
	}

	private static boolean hasNonEmptyCashType(Map<String, Object> input) {
		if (!input.containsKey("cash_type")) {
			return false;
		}
		Object raw = input.get("cash_type");
		if (raw == null) {
			return false;
		}
		return StringUtils.hasText(String.valueOf(raw).trim());
	}

	private static String extractCashAmtYuanString(Object raw) {
		if (raw instanceof BigDecimal bd) {
			return bd.stripTrailingZeros().toPlainString();
		}
		if (raw instanceof Number) {
			return new BigDecimal(raw.toString()).stripTrailingZeros().toPlainString();
		}
		return String.valueOf(raw).trim();
	}

	private static String checkCashAmtYuanToFen(String yuanStr) {
		if (!DISTRIBUTOR_MONEY_PATTERN.matcher(yuanStr).matches()) {
			throw new ResourceException("店铺账号提现金额必须是大于等于0的整数");
		}
		BigDecimal yuan;
		try {
			yuan = new BigDecimal(yuanStr);
		} catch (NumberFormatException e) {
			throw new ResourceException("店铺账号提现金额必须是大于等于0的整数");
		}
		BigDecimal fen;
		try {
			fen = yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException e) {
			throw new ResourceException("店铺账号提现金额必须是大于等于0的整数");
		}
		if (fen.compareTo(new BigDecimal("100000000")) > 0) {
			throw new ResourceException("店铺账号提现金额不能超过100万元");
		}
		return fen.toPlainString();
	}

	private static String stringify(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
