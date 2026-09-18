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

package cn.shopex.ecshopx.distribution.service.companys;

import cn.shopex.ecshopx.companys.service.deliverystaff.OperatorDeliveryStaffRoleDataService;
import cn.shopex.ecshopx.companys.service.operator.DistributorWorkWechatRelQueryService;
import cn.shopex.ecshopx.companys.service.operator.OperatorAdminAppDetailEnrichPort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorListRowFormatService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperatorAdminAppDetailEnrichPortImpl implements OperatorAdminAppDetailEnrichPort {

	private final DistributorMapper distributorMapper;

	private final DistributorListQueryService distributorListQueryService;

	private final DistributorListRowFormatService distributorListRowFormatService;

	private final SelfDeliverySettingReadService selfDeliverySettingReadService;

	private final DistributorSelfMetaService distributorSelfMetaService;

	private final DistributorWorkWechatRelQueryService distributorWorkWechatRelQueryService;

	private final OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService;

	private final ObjectMapper objectMapper;

	public OperatorAdminAppDetailEnrichPortImpl(
			DistributorMapper distributorMapper,
			DistributorListQueryService distributorListQueryService,
			DistributorListRowFormatService distributorListRowFormatService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorSelfMetaService distributorSelfMetaService,
			DistributorWorkWechatRelQueryService distributorWorkWechatRelQueryService,
			OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService,
			ObjectMapper objectMapper) {
		this.distributorMapper = distributorMapper;
		this.distributorListQueryService = distributorListQueryService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.distributorWorkWechatRelQueryService = distributorWorkWechatRelQueryService;
		this.operatorDeliveryStaffRoleDataService = operatorDeliveryStaffRoleDataService;
		this.objectMapper = objectMapper;
	}

	@Override
	public void enrichAppDetailForOperator(long companyId, LinkedHashMap<String, Object> operatorInfo) {
		decodeJsonColumnsIntoOperatorMap(operatorInfo);
		operatorInfo.put("distributors", new ArrayList<Map<String, Object>>());
		String operatorType = String.valueOf(operatorInfo.getOrDefault("operator_type", "")).trim();

		Object distCol = operatorInfo.get("distributor_ids");
		List<Map<String, Object>> distObjs = new ArrayList<>();
		if (distCol instanceof List<?> rawDist) {
			for (Object o : rawDist) {
				if (o instanceof Map<?, ?> m) {
					LinkedHashMap<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						if (e.getKey() instanceof String k) {
							row.put(k, e.getValue());
						}
					}
					distObjs.add(row);
				}
			}
		}

		LinkedHashSet<Long> idSet = new LinkedHashSet<>();
		boolean hasZeroId = false;
		for (Map<String, Object> el : distObjs) {
			if (el == null) {
				continue;
			}
			Object idRaw = el.get("distributor_id");
			String idStr = String.valueOf(idRaw != null ? idRaw : "").trim();
			if ("0".equals(idStr)) {
				hasZeroId = true;
			}
			Long id = parseLongLoose(idRaw);
			if (id != null) {
				idSet.add(id);
			}
		}
		List<Long> idList = new ArrayList<>(idSet);

		if (!idList.isEmpty()) {
			List<Distributor> entities = new ArrayList<>(distributorListQueryService.listByIdsAndCompany(companyId, idList));
			entities.sort(createdDescNullsLast());
			appendFormattedDistributors(companyId, entities, operatorInfo);
		} else if ("admin".equals(operatorType)) {
			LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
			w.eq(Distributor::getCompanyId, companyId).orderByDesc(Distributor::getCreated).last("LIMIT 100");
			List<Distributor> entities = distributorMapper.selectList(w);
			appendFormattedDistributors(companyId, entities, operatorInfo);
		}

		if (hasZeroId || "admin".equals(operatorType)) {
			Map<String, Object> selfDis = new LinkedHashMap<>(distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId));
			selfDis.put("is_center", Boolean.TRUE);
			Object distsObj = operatorInfo.get("distributors");
			if (distsObj instanceof List<?> listRaw) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> distributors = (List<Map<String, Object>>) listRaw;
				distributors.add(0, selfDis);
			}
		}

		Long opIdParsed = parseLongLoose(operatorInfo.get("operator_id"));
		long operatorId = opIdParsed != null ? opIdParsed : 0L;
		if (operatorId <= 0L) {
			operatorInfo.put("work_userid", "");
			operatorInfo.put("role_data", Collections.emptyList());
		} else {
			operatorInfo.put(
					"work_userid", distributorWorkWechatRelQueryService.findWorkUseridByCompanyAndOperator(companyId, operatorId));
			operatorInfo.put("role_data", operatorDeliveryStaffRoleDataService.getRoleDataList(companyId, operatorId));
		}
	}

	private void decodeJsonColumnsIntoOperatorMap(LinkedHashMap<String, Object> opMap) {
		Object shopRaw = opMap.get("shop_ids");
		if (shopRaw instanceof String s && StringUtils.hasText(s)) {
			opMap.put("shop_ids", decodeShopIdsValue(s));
		}
		Object distRaw = opMap.get("distributor_ids");
		if (distRaw instanceof String s && StringUtils.hasText(s)) {
			opMap.put("distributor_ids", decodeDistributorIdsJson(s));
		}
	}

	private Object decodeShopIdsValue(String raw) {
		try {
			Object parsed = objectMapper.readValue(raw, Object.class);
			if (parsed instanceof List<?> || parsed instanceof Map<?, ?>) {
				return parsed;
			}
			return new ArrayList<>();
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private List<Map<String, Object>> decodeDistributorIdsJson(String raw) {
		try {
			List<Object> parsed = objectMapper.readValue(raw, new TypeReference<List<Object>>() {});
			if (parsed == null) {
				return new ArrayList<>();
			}
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : parsed) {
				if (o instanceof Map<?, ?> m) {
					LinkedHashMap<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						if (e.getKey() instanceof String k) {
							row.put(k, e.getValue());
						}
					}
					out.add(row);
				}
			}
			return out;
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private void appendFormattedDistributors(long companyId, List<Distributor> entities, Map<String, Object> operatorInfo) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> distributors = (List<Map<String, Object>>) operatorInfo.get("distributors");
		for (Distributor d : entities) {
			long did = d.getDistributorId() != null ? d.getDistributorId() : 0L;
			int ds = d.getDistributorSelf() != null ? d.getDistributorSelf() : 0;
			Map<String, Object> setting = selfDeliverySettingReadService.getSetting(companyId, did, ds);
			Map<String, Object> row = distributorListRowFormatService.formatStoreRow(d, setting, objectMapper);
			row.put("is_center", Boolean.FALSE);
			distributors.add(row);
		}
	}

	private static Comparator<Distributor> createdDescNullsLast() {
		return (a, b) -> {
			Long ca = a.getCreated();
			Long cb = b.getCreated();
			if (ca == null && cb == null) {
				return 0;
			}
			if (ca == null) {
				return 1;
			}
			if (cb == null) {
				return -1;
			}
			return Long.compare(cb, ca);
		};
	}

	private static Long parseLongLoose(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return null;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				try {
					return new BigDecimal(t).longValue();
				} catch (Exception e2) {
					return null;
				}
			}
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			try {
				return new BigDecimal(s).longValue();
			} catch (Exception e2) {
				return null;
			}
		}
	}
}
