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

package cn.shopex.ecshopx.distribution.service.distributorvalid;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class DistributorIsValidCoreQueryService {

	private static final String IS_VALID_TRUE = "true";

	private final DistributorMapper distributorMapper;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;

	public DistributorIsValidCoreQueryService(
			DistributorMapper distributorMapper, SelfDeliverySettingReadService selfDeliverySettingReadService) {
		this.distributorMapper = distributorMapper;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
	}

	public Map<String, Object> getInfo(Map<String, Object> filter) {
		long companyId = ((Number) Objects.requireNonNull(filter.get("company_id"))).longValue();
		boolean hasDistributorIdKey = filter.containsKey("distributor_id");
		if (hasDistributorIdKey) {
			Object did = filter.get("distributor_id");
			if (did instanceof List<?> l && l.isEmpty()) {
				hasDistributorIdKey = false;
			}
		}
		if (hasDistributorIdKey) {
			Map<String, Object> q = buildDynamicQuery(filter);
			Map<String, Object> row = distributorMapper.selectDistributorRowDynamic(q);
			if (row != null && !row.isEmpty()) {
				DistributorIsValidRowKeyBridge.ensureSnakeCaseCoreKeys(row);
				applyFormatStoreInfo(row);
				return row;
			}
		}
		Map<String, Object> def = loadDefaultDistributorRow(companyId);
		if (def != null && !def.isEmpty()) {
			if (isInvalidOrNonSelfDefault(def)) {
				Map<String, Object> main = loadMainDistributorRow(companyId);
				if (main != null && !main.isEmpty()) {
					return main;
				}
			}
			return def;
		}
		return loadMainDistributorRow(companyId);
	}

	public Map<String, Object> getInfoSimple(long companyId, int isDefault, String isValid) {
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("company_id", companyId);
		q.put("is_default", isDefault);
		q.put("is_valid", isValid);
		Map<String, Object> row = distributorMapper.selectDistributorRowDynamic(q);
		if (row == null) {
			return null;
		}
		DistributorIsValidRowKeyBridge.ensureSnakeCaseCoreKeys(row);
		return row;
	}

	Map<String, Object> getInfoForNewestOpenDividedWhitelistDistributor(
			long companyId, List<Long> distributorIdList) {
		if (distributorIdList == null || distributorIdList.isEmpty()) {
			return new LinkedHashMap<>();
		}
		Distributor entity =
				distributorMapper.selectOne(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, companyId)
								.in(Distributor::getDistributorId, distributorIdList)
								.eq(Distributor::getOpenDivided, 1L)
								.orderByDesc(Distributor::getCreated)
								.last("LIMIT 1"));
		if (entity == null) {
			return new LinkedHashMap<>();
		}
		Map<String, Object> f = new LinkedHashMap<>();
		f.put("company_id", companyId);
		f.put("distributor_id", entity.getDistributorId());
		f.put("open_divided", Integer.valueOf(1));
		Map<String, Object> row = getInfo(f);
		return row == null ? new LinkedHashMap<>() : row;
	}

	public Map<String, Object> getDistributorSelf(
			long companyId, boolean returnFullInfo, Map<String, Object> isOpenDividedExtra) {
		Map<String, Object> row;
		if (isOpenDividedExtra == null || isOpenDividedExtra.isEmpty()) {
			Map<String, Object> q = new LinkedHashMap<>();
			q.put("company_id", companyId);
			q.put("distributor_self", 1);
			row = distributorMapper.selectDistributorRowDynamic(q);
		} else {
			Object od = isOpenDividedExtra.get("open_divided");
			if (od instanceof Integer i && i == 0) {
				Map<String, Object> q = new LinkedHashMap<>();
				q.put("company_id", companyId);
				q.put("distributor_self", 0);
				q.put("open_divided", 0);
				q.put("is_valid", IS_VALID_TRUE);
				row = distributorMapper.selectDistributorRowDynamic(q);
				if (row == null || row.isEmpty()) {
					Map<String, Object> q2 = new LinkedHashMap<>();
					q2.put("company_id", companyId);
					q2.put("is_default", 1);
					q2.put("is_valid", IS_VALID_TRUE);
					row = distributorMapper.selectDistributorRowDynamic(q2);
					if (row != null && !row.isEmpty()) {
						row.put("white_hidden", 1);
					}
				}
			} else {
				Map<String, Object> q = new LinkedHashMap<>();
				q.put("company_id", companyId);
				q.put("distributor_self", 1);
				row = distributorMapper.selectDistributorRowDynamic(q);
			}
		}
		if (row == null || row.isEmpty()) {
			return new LinkedHashMap<>();
		}
		DistributorIsValidRowKeyBridge.ensureSnakeCaseCoreKeys(row);
		if (returnFullInfo) {
			applyFormatStoreInfo(row);
		}
		return row;
	}

	void applyFormatStoreInfo(Map<String, Object> row) {
		String province = stringVal(row.get("province"));
		String city = stringVal(row.get("city"));
		String area = stringVal(row.get("area"));
		String address = stringVal(row.get("address"));
		if ("上海市".equals(province)
				|| "北京市".equals(province)
				|| "重庆市".equals(province)
				|| "天津市".equals(province)) {
			row.put("store_address", province + area + address);
		} else {
			row.put("store_address", province + city + area + address);
		}
		row.put("store_name", stringVal(row.get("name")));
		row.put("phone", row.get("mobile"));
		Object rate = row.get("rate");
		if (rate instanceof Number n && n.intValue() != 0) {
			BigDecimal v = BigDecimal.valueOf(n.longValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			row.put("rate", v.toPlainString());
		} else {
			row.put("rate", "");
		}
		numericStringToBoolean(row, "is_ziti");
		numericStringToBoolean(row, "is_delivery");
		numericStringToBoolean(row, "is_open_salesman");
		numericStringToBoolean(row, "is_default");
		numericStringToBoolean(row, "is_self_delivery");
		numericStringToBoolean(row, "auto_sync_goods");
		numericStringToBoolean(row, "is_audit_goods");
		numericStringToBoolean(row, "is_distributor");
		numericStringToBoolean(row, "review_status");
		if (row.get("is_dada") != null) {
			row.put("is_dada", toIntLoose(row.get("is_dada")));
		}
		numericStringToBoolean(row, "dada_shop_create");
		numericStringToBoolean(row, "is_require_subdistrict");
		numericStringToBoolean(row, "is_require_building");
		Map<String, Object> selfDeliveryRule =
				selfDeliverySettingReadService.getSetting(
						((Number) row.get("company_id")).longValue(),
						((Number) row.get("distributor_id")).longValue(),
						DistributorIsValidDistributorSelfParsing.distributorSelfColumnToIntForGetSetting(
								row.get("distributor_self")));
		row.put("selfDeliveryRule", selfDeliveryRule);
	}

	private Map<String, Object> loadDefaultDistributorRow(long companyId) {
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("company_id", companyId);
		q.put("is_default", 1);
		Map<String, Object> row = distributorMapper.selectDistributorRowDynamic(q);
		if (row == null || row.isEmpty()) {
			return null;
		}
		DistributorIsValidRowKeyBridge.ensureSnakeCaseCoreKeys(row);
		applyFormatStoreInfo(row);
		return row;
	}

	private Map<String, Object> loadMainDistributorRow(long companyId) {
		Map<String, Object> q = new LinkedHashMap<>();
		q.put("company_id", companyId);
		q.put("distributor_self", 1);
		Map<String, Object> row = distributorMapper.selectDistributorRowDynamic(q);
		if (row == null || row.isEmpty()) {
			return new LinkedHashMap<>();
		}
		DistributorIsValidRowKeyBridge.ensureSnakeCaseCoreKeys(row);
		applyFormatStoreInfo(row);
		row.put("distributor_id", 0L);
		return row;
	}

	private boolean isInvalidOrNonSelfDefault(Map<String, Object> def) {
		Object iv = def.get("is_valid");
		String s = iv == null ? "" : String.valueOf(iv).trim();
		if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
			return true;
		}
		Object ds = def.get("distributor_self");
		if (ds instanceof Number n) {
			return n.intValue() == 0;
		}
		if (ds instanceof String t) {
			return "0".equals(t.trim());
		}
		return false;
	}

	private static Map<String, Object> buildDynamicQuery(Map<String, Object> filter) {
		Map<String, Object> q = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : filter.entrySet()) {
			if ("distributor_id".equals(e.getKey()) && e.getValue() instanceof List<?> l) {
				List<?> raw = l;
				List<Long> ids = new ArrayList<>(raw.size());
				for (Object o : raw) {
					if (o instanceof Number n) {
						ids.add(n.longValue());
					} else if (o != null) {
						ids.add(Long.parseLong(String.valueOf(o).trim()));
					}
				}
				q.put("distributor_id_in", ids);
			} else {
				q.put(e.getKey(), e.getValue());
			}
		}
		return q;
	}

	private static void numericStringToBoolean(Map<String, Object> row, String key) {
		Object v = row.get(key);
		if (v == null) {
			return;
		}
		if (v instanceof Boolean) {
			return;
		}
		if (v instanceof Number n) {
			row.put(key, n.intValue() == 1);
			return;
		}
		String s = String.valueOf(v).trim();
		row.put(key, "1".equals(s));
	}

	private static int toIntLoose(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}
}
