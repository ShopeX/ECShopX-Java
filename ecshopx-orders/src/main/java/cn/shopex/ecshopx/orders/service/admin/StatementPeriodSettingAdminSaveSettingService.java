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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.distribution.DistributorGetInfoSimpleByDistributorIdPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.orders.domain.StatementPeriodSetting;
import cn.shopex.ecshopx.orders.mapper.StatementPeriodSettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class StatementPeriodSettingAdminSaveSettingService {

	private static final Set<String> PERIOD_UNITS = Set.of("day", "week", "month");

	private final StatementPeriodSettingMapper statementPeriodSettingMapper;
	private final DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort;
	private final ObjectMapper objectMapper;

	public StatementPeriodSettingAdminSaveSettingService(
			StatementPeriodSettingMapper statementPeriodSettingMapper,
			DistributorGetInfoSimpleByDistributorIdPort distributorGetInfoSimpleByDistributorIdPort,
			ObjectMapper objectMapper) {
		this.statementPeriodSettingMapper = statementPeriodSettingMapper;
		this.distributorGetInfoSimpleByDistributorIdPort = distributorGetInfoSimpleByDistributorIdPort;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> saveSetting(long companyId, Map<String, Object> merged) {
		Long idOpt = EspierAdminJwtControllerSupport.parseLongOrNull(merged.get("id"));

		Long d = EspierAdminJwtControllerSupport.parseLongOrNull(merged.get("distributor_id"));
		long distributorId = (d == null || d < 0L) ? 0L : d;

		Long s = EspierAdminJwtControllerSupport.parseLongOrNull(merged.get("supplier_id"));
		long supplierId = (s == null || s < 0L) ? 0L : s;

		String mtRaw = EspierAdminJwtControllerSupport.optionalTrimmedString(merged.get("merchant_type"));
		String merchantType = (mtRaw == null || mtRaw.isEmpty()) ? "distributor" : mtRaw;

		boolean distributorScope = "distributor".equals(merchantType);

		String periodJson = buildPeriodJson(merged.get("period"));

		long merchantId = 0L;
		if (distributorId > 0L) {
			Map<String, Object> dist =
					distributorGetInfoSimpleByDistributorIdPort.getInfoSimpleByDistributorId(companyId, distributorId);
			if (dist.isEmpty()) {
				throw new ResourceException("店铺不存在");
			}
			Long parsed = EspierAdminJwtControllerSupport.parseLongOrNull(dist.get("merchant_id"));
			merchantId = parsed == null ? 0L : parsed;
		}

		LambdaQueryWrapper<StatementPeriodSetting> wrapper = new LambdaQueryWrapper<StatementPeriodSetting>()
				.eq(StatementPeriodSetting::getCompanyId, companyId);
		if (distributorScope) {
			wrapper.eq(StatementPeriodSetting::getMerchantType, "distributor")
					.eq(StatementPeriodSetting::getDistributorId, distributorId);
		} else {
			wrapper.eq(StatementPeriodSetting::getMerchantType, "supplier")
					.eq(StatementPeriodSetting::getSupplierId, supplierId);
		}
		StatementPeriodSetting existing = statementPeriodSettingMapper.selectOne(wrapper);

		int now = (int) (System.currentTimeMillis() / 1000);

		if (idOpt != null && idOpt > 0L) {
			if (existing != null && !existing.getId().equals(idOpt)) {
				throw new ResourceException(
						distributorScope ? "每个店铺只能配置一个结算周期" : "每个供应商只能配置一个结算周期");
			}
			StatementPeriodSetting row = statementPeriodSettingMapper.selectById(idOpt);
			if (row == null) {
				throw new ResourceException("未查询到更新数据");
			}
			applyBusinessFields(row, companyId, merchantId, distributorId, supplierId, merchantType, periodJson, now, false);
			statementPeriodSettingMapper.updateById(row);
			return toApiRow(row);
		}

		if (existing != null) {
			applyBusinessFields(
					existing, companyId, merchantId, distributorId, supplierId, merchantType, periodJson, now, false);
			statementPeriodSettingMapper.updateById(existing);
			return toApiRow(existing);
		}

		StatementPeriodSetting entity = new StatementPeriodSetting();
		applyBusinessFields(
				entity, companyId, merchantId, distributorId, supplierId, merchantType, periodJson, now, true);
		statementPeriodSettingMapper.insert(entity);
		return toApiRow(entity);
	}

	private String buildPeriodJson(Object periodRaw) {
		List<?> list = normalizePeriodList(periodRaw);
		if (list == null || list.size() != 2) {
			throw new BadRequestException("结算周期设置错误");
		}
		Long periodNumber = EspierAdminJwtControllerSupport.parseLongOrNull(list.get(0));
		if (periodNumber == null || periodNumber <= 0L) {
			throw new BadRequestException("结算周期设置错误");
		}
		String unitRaw = EspierAdminJwtControllerSupport.optionalTrimmedString(list.get(1));
		if (unitRaw == null) {
			throw new BadRequestException("结算周期设置错误");
		}
		String periodUnit = unitRaw.toLowerCase(Locale.ROOT);
		if (!PERIOD_UNITS.contains(periodUnit)) {
			throw new BadRequestException("结算周期设置错误");
		}
		try {
			return objectMapper.writeValueAsString(List.of(periodNumber, periodUnit));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("结算周期格式错误");
		}
	}

	private List<?> normalizePeriodList(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof List<?> l) {
			return l;
		}
		if (raw instanceof Object[] arr) {
			return Arrays.asList(arr);
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return null;
			}
			try {
				return objectMapper.readValue(t, new TypeReference<List<Object>>() {});
			} catch (JsonProcessingException e) {
				return null;
			}
		}
		return null;
	}

	private static void applyBusinessFields(
			StatementPeriodSetting row,
			long companyId,
			long merchantId,
			long distributorId,
			long supplierId,
			String merchantType,
			String periodJson,
			int now,
			boolean touchCreated) {
		row.setCompanyId(companyId);
		row.setMerchantId(merchantId);
		row.setDistributorId(distributorId);
		row.setSupplierId(supplierId);
		row.setMerchantType(merchantType);
		row.setPeriod(periodJson);
		row.setUpdated(now);
		if (touchCreated) {
			row.setCreated(now);
		}
	}

	private Map<String, Object> toApiRow(StatementPeriodSetting e) {
		String periodStr = e.getPeriod();
		if (periodStr == null || periodStr.isEmpty()) {
			throw new BadRequestException("结算周期格式错误");
		}
		List<Object> periodList;
		try {
			periodList = objectMapper.readValue(periodStr, new TypeReference<List<Object>>() {});
		} catch (JsonProcessingException ex) {
			throw new BadRequestException("结算周期格式错误");
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("merchant_id", e.getMerchantId());
		m.put("distributor_id", e.getDistributorId());
		m.put("period", periodList);
		m.put("supplier_id", e.getSupplierId());
		m.put("merchant_type", e.getMerchantType());
		return m;
	}
}
