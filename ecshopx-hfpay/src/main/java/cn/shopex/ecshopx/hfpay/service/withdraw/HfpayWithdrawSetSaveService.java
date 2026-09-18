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

package cn.shopex.ecshopx.hfpay.service.withdraw;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.domain.HfpayWithdrawSet;
import cn.shopex.ecshopx.hfpay.mapper.HfpayWithdrawSetMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class HfpayWithdrawSetSaveService {

	private static final DateTimeFormatter HFPAY_ADMIN_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final Pattern DISTRIBUTOR_MONEY_PATTERN = Pattern.compile(
			"^(([0-9]+.[0-9]*[1-9][0-9]*)|([0-9]*[1-9][0-9]*.[0-9]+)|([0-9]*[1-9][0-9]*))|0?.0+|0$");

	private final HfpayWithdrawSetMapper hfpayWithdrawSetMapper;

	public HfpayWithdrawSetSaveService(HfpayWithdrawSetMapper hfpayWithdrawSetMapper) {
		this.hfpayWithdrawSetMapper = hfpayWithdrawSetMapper;
	}

	public Map<String, Object> save(long companyId, Map<String, Object> params) {
		validateRequiredParams(companyId, params);

		int withdrawMethod = parseWithdrawMethod(params.get("withdraw_method"));
		String yuanStr = toYuanString(params.get("distributor_money"));
		String fenString = checkMoneyFormatAndToFen(yuanStr);

		LambdaQueryWrapper<HfpayWithdrawSet> w =
				new LambdaQueryWrapper<HfpayWithdrawSet>().eq(HfpayWithdrawSet::getCompanyId, companyId).last("LIMIT 1");
		HfpayWithdrawSet existing = hfpayWithdrawSetMapper.selectOne(w);

		if (existing != null) {
			Long id = existing.getHfpayWithdrawSetId();
			HfpayWithdrawSet row = hfpayWithdrawSetMapper.selectById(id);
			if (row == null) {
				throw new ResourceException("未查询到更新数据");
			}
			row.setWithdrawMethod(withdrawMethod);
			row.setDistributorMoney(fenString);
			row.setUpdatedAt(LocalDateTime.now());
			hfpayWithdrawSetMapper.updateById(row);
			HfpayWithdrawSet refreshed = hfpayWithdrawSetMapper.selectById(id);
			return toSnakeColumnMap(refreshed);
		}

		HfpayWithdrawSet entity = new HfpayWithdrawSet();
		entity.setCompanyId(companyId);
		entity.setWithdrawMethod(withdrawMethod);
		entity.setDistributorMoney(fenString);
		LocalDateTime now = LocalDateTime.now();
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);
		hfpayWithdrawSetMapper.insert(entity);
		HfpayWithdrawSet inserted = hfpayWithdrawSetMapper.selectById(entity.getHfpayWithdrawSetId());
		return toSnakeColumnMap(inserted);
	}

	public Map<String, Object> getWithdrawSet(long companyId) {
		LambdaQueryWrapper<HfpayWithdrawSet> w =
				new LambdaQueryWrapper<HfpayWithdrawSet>().eq(HfpayWithdrawSet::getCompanyId, companyId).last("LIMIT 1");
		HfpayWithdrawSet row = hfpayWithdrawSetMapper.selectOne(w);
		if (row == null) {
			return null;
		}
		return toSnakeColumnMapForRead(row);
	}

	private void validateRequiredParams(long companyId, Map<String, Object> params) {
		if (companyId <= 0L) {
			throw new BadRequestException("企业id必填");
		}
		if (!params.containsKey("withdraw_method") || isMissingOrBlank(params.get("withdraw_method"))) {
			throw new BadRequestException("请选择提现方式");
		}
		if (!params.containsKey("distributor_money") || isMissingOrBlank(params.get("distributor_money"))) {
			throw new BadRequestException("店铺账号提现金额必填");
		}
	}

	private static boolean isMissingOrBlank(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return s.trim().isEmpty();
		}
		if (v instanceof CharSequence cs) {
			return cs.toString().trim().isEmpty();
		}
		return false;
	}

	private static int parseWithdrawMethod(Object raw) {
		try {
			if (raw instanceof Number n) {
				int v = n.intValue();
				if (v < 0) {
					throw new BadRequestException("请选择提现方式");
				}
				return v;
			}
			String s = String.valueOf(raw).trim();
			int v = Integer.parseInt(s);
			if (v < 0) {
				throw new BadRequestException("请选择提现方式");
			}
			return v;
		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("请选择提现方式");
		}
	}

	private static String toYuanString(Object v) {
		if (v instanceof BigDecimal bd) {
			return bd.stripTrailingZeros().toPlainString();
		}
		if (v instanceof Number n) {
			return new BigDecimal(n.toString()).stripTrailingZeros().toPlainString();
		}
		return String.valueOf(v).trim();
	}

	private static String checkMoneyFormatAndToFen(String yuanStr) {
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

	private static Map<String, Object> nestedAdminDateTimeMap(LocalDateTime t) {
		if (t == null) {
			return null;
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("date", HFPAY_ADMIN_TS.format(t) + ".000000");
		m.put("timezone_type", 3);
		m.put("timezone", "PRC");
		return m;
	}

	private static Map<String, Object> toSnakeColumnMap(HfpayWithdrawSet e) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("hfpay_withdraw_set_id", e.getHfpayWithdrawSetId());
		map.put("company_id", e.getCompanyId());
		map.put("withdraw_method", e.getWithdrawMethod());
		map.put("distributor_money", e.getDistributorMoney());
		map.put("created_at", nestedAdminDateTimeMap(e.getCreatedAt()));
		map.put("updated_at", nestedAdminDateTimeMap(e.getUpdatedAt()));
		return map;
	}

	private static Map<String, Object> toSnakeColumnMapForRead(HfpayWithdrawSet e) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("hfpay_withdraw_set_id", e.getHfpayWithdrawSetId());
		map.put("company_id", e.getCompanyId());
		map.put("withdraw_method", e.getWithdrawMethod());
		map.put("distributor_money", distributorMoneyFenToYuanPlain(e.getDistributorMoney()));
		map.put("created_at", nestedAdminDateTimeMap(e.getCreatedAt()));
		map.put("updated_at", nestedAdminDateTimeMap(e.getUpdatedAt()));
		return map;
	}

	private static String distributorMoneyFenToYuanPlain(String fenRaw) {
		String s = fenRaw == null || fenRaw.isBlank() ? "0" : fenRaw.trim();
		BigDecimal fen;
		try {
			fen = new BigDecimal(s);
		} catch (NumberFormatException ex) {
			fen = BigDecimal.ZERO;
		}
		return fen.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toPlainString();
	}
}
