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
import cn.shopex.ecshopx.distribution.repository.BasicConfigWriteRepository;
import cn.shopex.ecshopx.distribution.support.BasicConfigColumnNamesDataMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BasicConfigSaveService {

	private static final String NO_UPDATE_DATA = "未查询到更新数据";

	private static final String IS_INCOME_TAX_FALSE = "0";

	private final BasicConfigWriteRepository basicConfigWriteRepository;

	public BasicConfigSaveService(BasicConfigWriteRepository basicConfigWriteRepository) {
		this.basicConfigWriteRepository = basicConfigWriteRepository;
	}

	public Map<String, Object> saveBasicConfig(long companyId, Map<String, Object> params) {
		BasicConfig existing = basicConfigWriteRepository.getInfoByCompanyId(companyId);
		boolean applyLimitRebate = shouldApplyLimitRebateColumn(params);
		String limitRebateStored = computeLimitRebateStoredString(params);

		String limitTime = normalizeLimitTime(params.get("limit_time"));
		String returnName = stringVal(params.get("return_name"));
		String returnAddress = stringVal(params.get("return_address"));
		String returnPhone = stringVal(params.get("return_phone"));

		if (existing != null) {
			BasicConfig patch = new BasicConfig();
			patch.setIsBuy(Boolean.TRUE);
			patch.setLimitTime(limitTime);
			patch.setReturnName(returnName);
			patch.setReturnAddress(returnAddress);
			patch.setReturnPhone(returnPhone);
			patch.setIsIncomeTax(IS_INCOME_TAX_FALSE);
			if (applyLimitRebate) {
				patch.setLimitRebate(limitRebateStored);
			}
			int affected = basicConfigWriteRepository.updateOneByCompanyId(companyId, patch, applyLimitRebate);
			if (affected == 0) {
				throw new ResourceException(NO_UPDATE_DATA);
			}
		} else {
			BasicConfig entity = new BasicConfig();
			entity.setCompanyId(companyId);
			entity.setIsBuy(Boolean.TRUE);
			entity.setLimitTime(limitTime);
			entity.setReturnName(returnName);
			entity.setReturnAddress(returnAddress);
			entity.setReturnPhone(returnPhone);
			entity.setIsIncomeTax(IS_INCOME_TAX_FALSE);
			if (applyLimitRebate) {
				entity.setLimitRebate(limitRebateStored);
			}
			basicConfigWriteRepository.create(entity);
		}

		BasicConfig row = basicConfigWriteRepository.getInfoByCompanyId(companyId);
		return BasicConfigColumnNamesDataMapper.toColumnNamesData(row);
	}

	private static boolean shouldApplyLimitRebateColumn(Map<String, Object> params) {
		Object raw = params.get("limit_rebate");
		if (raw == null) {
			return false;
		}
		String s = raw.toString().trim();
		return StringUtils.hasText(s) && !"0".equals(s);
	}

	private static String computeLimitRebateStoredString(Map<String, Object> params) {
		Object raw = params.get("limit_rebate");
		if (!limitRebateTruthyForBcmul(raw)) {
			return "0";
		}
		String num = normalizeDecimalString(raw);
		try {
			return new BigDecimal(num)
					.multiply(BigDecimal.valueOf(100))
					.setScale(0, RoundingMode.HALF_UP)
					.toPlainString();
		} catch (NumberFormatException ex) {
			throw new BadRequestException("limit_rebate 格式不正确");
		}
	}

	private static boolean limitRebateTruthyForBcmul(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return false;
		}
		if ("0".equals(s)) {
			return false;
		}
		return true;
	}

	private static String normalizeDecimalString(Object raw) {
		if (raw instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue()).toPlainString();
		}
		return raw.toString().trim();
	}

	private static String normalizeLimitTime(Object raw) {
		if (raw == null) {
			return "0";
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return "0";
		}
		try {
			new BigDecimal(s);
			return s;
		} catch (NumberFormatException e) {
			return "0";
		}
	}

	private static String stringVal(Object v) {
		return v == null ? null : v.toString();
	}
}
