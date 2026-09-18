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

package cn.shopex.ecshopx.hfpay.service.ledger;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.domain.HfpayLedgerConfig;
import cn.shopex.ecshopx.hfpay.mapper.HfpayLedgerConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayLedgerConfigSaveService {

	private static final DateTimeFormatter HFPAY_ADMIN_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final HfpayLedgerConfigMapper hfpayLedgerConfigMapper;

	public HfpayLedgerConfigSaveService(HfpayLedgerConfigMapper hfpayLedgerConfigMapper) {
		this.hfpayLedgerConfigMapper = hfpayLedgerConfigMapper;
	}

	public Map<String, Object> getLedgerConfigForIndex(long companyId) {
		if (companyId <= 0L) {
			throw new BadRequestException("企业id必填");
		}
		LambdaQueryWrapper<HfpayLedgerConfig> q = new LambdaQueryWrapper<>();
		q.eq(HfpayLedgerConfig::getCompanyId, companyId).last("LIMIT 1");
		HfpayLedgerConfig row = hfpayLedgerConfigMapper.selectOne(q);
		if (row == null) {
			return null;
		}
		return toIndexColumnMap(row);
	}

	public Map<String, Object> save(long companyId, Map<String, Object> params) {
		validateParams(companyId, params);

		LambdaQueryWrapper<HfpayLedgerConfig> q = new LambdaQueryWrapper<>();
		q.eq(HfpayLedgerConfig::getCompanyId, companyId).last("LIMIT 1");
		HfpayLedgerConfig existingRow = hfpayLedgerConfigMapper.selectOne(q);
		if (existingRow != null) {
			params.put("hfpay_ledger_config_id", existingRow.getHfpayLedgerConfigId());
		}

		checkAndNormalizeRate(params);

		Object idRaw = params.get("hfpay_ledger_config_id");
		if (idRaw != null && StringUtils.hasText(String.valueOf(idRaw).trim())) {
			long id = parsePositiveLong(idRaw, "hfpay_ledger_config_id");
			HfpayLedgerConfig current = hfpayLedgerConfigMapper.selectById(id);
			if (current == null) {
				throw new ResourceException("未查询到更新数据");
			}
			LambdaUpdateWrapper<HfpayLedgerConfig> w = new LambdaUpdateWrapper<>();
			w.eq(HfpayLedgerConfig::getHfpayLedgerConfigId, id);
			applyPartialUpdateSets(w, params);
			w.set(HfpayLedgerConfig::getUpdatedAt, LocalDateTime.now());
			hfpayLedgerConfigMapper.update(null, w);
			HfpayLedgerConfig refreshed = hfpayLedgerConfigMapper.selectById(id);
			return toColumnMap(refreshed);
		}

		HfpayLedgerConfig entity = buildForInsert(companyId, params);
		entity.setCreatedAt(LocalDateTime.now());
		entity.setUpdatedAt(LocalDateTime.now());
		hfpayLedgerConfigMapper.insert(entity);
		HfpayLedgerConfig inserted = hfpayLedgerConfigMapper.selectById(entity.getHfpayLedgerConfigId());
		return toColumnMap(inserted);
	}

	private void validateParams(long companyId, Map<String, Object> params) {
		if (companyId <= 0L) {
			throw new BadRequestException("企业id必填");
		}
		if (!isSet(params, "rate") || !StringUtils.hasText(trimParamString(params.get("rate")))) {
			throw new BadRequestException("费率必填");
		}
		String businessType = normalizeBusinessTypeForValidator(params.get("business_type"));
		if ("2".equals(businessType)) {
			if (!isSet(params, "agent_number") || !StringUtils.hasText(trimParamString(params.get("agent_number")))) {
				throw new BadRequestException("代理商商户号必填");
			}
			if (!isSet(params, "provider_number") || !StringUtils.hasText(trimParamString(params.get("provider_number")))) {
				throw new BadRequestException("服务商渠道号必填");
			}
			if (!isSet(params, "app_id") || !StringUtils.hasText(trimParamString(params.get("app_id")))) {
				throw new BadRequestException("小程序appID必填");
			}
		}
	}

	private void checkAndNormalizeRate(Map<String, Object> params) {
		if (!isBusinessTypeAllowed(params.get("business_type"))) {
			throw new BadRequestException("不支持的业务模式");
		}

		Object rateRaw = params.get("rate");
		int rateStorage = toRateStorageInt(rateRaw);
		if (rateStorage > 3000) {
			throw new BadRequestException("服务费率不得超过30");
		}
		params.put("rate", rateStorage);

		String bt = normalizeBusinessTypeForValidator(params.get("business_type"));
		if ("2".equals(bt)) {
			if (!StringUtils.hasText(trimParamString(params.get("agent_number")))) {
				throw new BadRequestException("代理商商户号不能为空");
			}
			if (!StringUtils.hasText(trimParamString(params.get("provider_number")))) {
				throw new BadRequestException("服务商渠道号不能为空");
			}
			if (!StringUtils.hasText(trimParamString(params.get("app_id")))) {
				throw new BadRequestException("小程序appid不能为空");
			}
		}
	}

	private boolean isBusinessTypeAllowed(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			return v == 1 || v == 2;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return false;
		}
		try {
			int v = Integer.parseInt(s);
			return v == 1 || v == 2;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private String normalizeBusinessTypeForValidator(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof Number n) {
			return String.valueOf(n.intValue());
		}
		return String.valueOf(raw).trim();
	}

	private int toRateStorageInt(Object rateRaw) {
		BigDecimal bd;
		if (rateRaw instanceof Number n) {
			bd = BigDecimal.valueOf(n.doubleValue());
		} else {
			String s = String.valueOf(rateRaw).trim();
			if (s.isEmpty()) {
				throw new BadRequestException("费率必填");
			}
			try {
				bd = new BigDecimal(s);
			} catch (NumberFormatException e) {
				throw new BadRequestException("费率必填");
			}
		}
		return bd.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private void applyPartialUpdateSets(LambdaUpdateWrapper<HfpayLedgerConfig> w, Map<String, Object> params) {
		if (isSet(params, "company_id")) {
			w.set(HfpayLedgerConfig::getCompanyId, parsePositiveLong(params.get("company_id"), "company_id"));
		}
		if (isSet(params, "is_open")) {
			w.set(HfpayLedgerConfig::getIsOpen, String.valueOf(params.get("is_open")).trim());
		}
		if (isSet(params, "business_type")) {
			w.set(HfpayLedgerConfig::getBusinessType, normalizeBusinessTypeForValidator(params.get("business_type")));
		}
		if (isSet(params, "agent_number")) {
			w.set(HfpayLedgerConfig::getAgentNumber, nullToEmptyString(params.get("agent_number")));
		}
		if (isSet(params, "provider_number")) {
			w.set(HfpayLedgerConfig::getProviderNumber, nullToEmptyString(params.get("provider_number")));
		}
		if (isSet(params, "rate")) {
			Object r = params.get("rate");
			w.set(HfpayLedgerConfig::getRate, r instanceof Number ? ((Number) r).intValue() : Integer.parseInt(String.valueOf(r).trim()));
		}
		if (isSet(params, "app_id")) {
			w.set(HfpayLedgerConfig::getAppId, nullToEmptyString(params.get("app_id")));
		}
	}

	private HfpayLedgerConfig buildForInsert(long companyId, Map<String, Object> params) {
		HfpayLedgerConfig e = new HfpayLedgerConfig();
		e.setCompanyId(companyId);
		if (isSet(params, "is_open")) {
			e.setIsOpen(String.valueOf(params.get("is_open")).trim());
		}
		if (isSet(params, "business_type")) {
			e.setBusinessType(normalizeBusinessTypeForValidator(params.get("business_type")));
		}
		if (isSet(params, "agent_number")) {
			e.setAgentNumber(nullToEmptyString(params.get("agent_number")));
		}
		if (isSet(params, "provider_number")) {
			e.setProviderNumber(nullToEmptyString(params.get("provider_number")));
		}
		Object r = params.get("rate");
		e.setRate(r instanceof Number ? ((Number) r).intValue() : Integer.parseInt(String.valueOf(r).trim()));
		if (isSet(params, "app_id")) {
			e.setAppId(nullToEmptyString(params.get("app_id")));
		}
		return e;
	}

	private static boolean isSet(Map<String, Object> params, String key) {
		return params.containsKey(key) && params.get(key) != null;
	}

	private static String trimParamString(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static String nullToEmptyString(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static long parsePositiveLong(Object raw, String fieldLabel) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数格式错误: " + fieldLabel);
		}
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

	private static Map<String, Object> toColumnMap(HfpayLedgerConfig row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("hfpay_ledger_config_id", row.getHfpayLedgerConfigId());
		m.put("company_id", row.getCompanyId());
		m.put("is_open", row.getIsOpen());
		m.put("business_type", row.getBusinessType());
		m.put("agent_number", row.getAgentNumber());
		m.put("provider_number", row.getProviderNumber());
		m.put("rate", row.getRate());
		m.put("app_id", row.getAppId());
		m.put("created_at", nestedAdminDateTimeMap(row.getCreatedAt()));
		m.put("updated_at", nestedAdminDateTimeMap(row.getUpdatedAt()));
		return m;
	}

	private static Map<String, Object> toIndexColumnMap(HfpayLedgerConfig row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("hfpay_ledger_config_id", row.getHfpayLedgerConfigId());
		m.put("company_id", row.getCompanyId());
		m.put("is_open", row.getIsOpen());
		m.put("business_type", row.getBusinessType());
		m.put("agent_number", row.getAgentNumber());
		m.put("provider_number", row.getProviderNumber());
		String rateDisplay = BigDecimal.valueOf(row.getRate() == null ? 0L : row.getRate().longValue())
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
		m.put("rate", rateDisplay);
		m.put("app_id", row.getAppId());
		m.put("created_at", nestedAdminDateTimeMap(row.getCreatedAt()));
		m.put("updated_at", nestedAdminDateTimeMap(row.getUpdatedAt()));
		return m;
	}
}
