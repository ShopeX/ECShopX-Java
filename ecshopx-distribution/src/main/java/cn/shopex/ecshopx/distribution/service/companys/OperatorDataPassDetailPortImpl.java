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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.OperatorDataPass;
import cn.shopex.ecshopx.companys.mapper.OperatorDataPassMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassDetailPort;
import cn.shopex.ecshopx.companys.service.datapass.OperatorDataPassExPassFormatter;
import cn.shopex.ecshopx.companys.service.deliverystaff.OperatorDeliveryStaffRoleDataService;
import cn.shopex.ecshopx.companys.service.operator.DistributorWorkWechatRelQueryService;
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
public class OperatorDataPassDetailPortImpl implements OperatorDataPassDetailPort {

	private final OperatorDataPassMapper operatorDataPassMapper;
	private final OperatorDataPassExPassFormatter operatorDataPassExPassFormatter;
	private final OperatorsQueryService operatorsQueryService;
	private final DistributorListQueryService distributorListQueryService;
	private final DistributorMapper distributorMapper;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorSelfMetaService distributorSelfMetaService;
	private final DistributorWorkWechatRelQueryService distributorWorkWechatRelQueryService;
	private final OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService;
	private final ObjectMapper objectMapper;

	public OperatorDataPassDetailPortImpl(
			OperatorDataPassMapper operatorDataPassMapper,
			OperatorDataPassExPassFormatter operatorDataPassExPassFormatter,
			OperatorsQueryService operatorsQueryService,
			DistributorListQueryService distributorListQueryService,
			DistributorMapper distributorMapper,
			DistributorListRowFormatService distributorListRowFormatService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorSelfMetaService distributorSelfMetaService,
			DistributorWorkWechatRelQueryService distributorWorkWechatRelQueryService,
			OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService,
			ObjectMapper objectMapper) {
		this.operatorDataPassMapper = operatorDataPassMapper;
		this.operatorDataPassExPassFormatter = operatorDataPassExPassFormatter;
		this.operatorsQueryService = operatorsQueryService;
		this.distributorListQueryService = distributorListQueryService;
		this.distributorMapper = distributorMapper;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorSelfMetaService = distributorSelfMetaService;
		this.distributorWorkWechatRelQueryService = distributorWorkWechatRelQueryService;
		this.operatorDeliveryStaffRoleDataService = operatorDeliveryStaffRoleDataService;
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, Object> fetchDataPassDetail(long passId, String callerOperatorType) {
		OperatorDataPass row =
				operatorDataPassMapper.selectOne(
						new LambdaQueryWrapper<OperatorDataPass>().eq(OperatorDataPass::getPassId, passId));
		if (row == null) {
			throw new ResourceException("数据不存在");
		}

		LinkedHashMap<String, Object> detail = new LinkedHashMap<>();
		putLong(detail, "pass_id", row.getPassId());
		putLong(detail, "company_id", row.getCompanyId());
		detail.put("operator_id", row.getOperatorId());
		detail.put("status", row.getStatus());
		detail.put("is_closed", row.getIsClosed());
		detail.put("reason", row.getReason() == null ? "" : row.getReason());
		detail.put("remarks", row.getRemarks() == null ? "" : row.getRemarks());
		detail.put("create_time", row.getCreateTime());
		detail.put("approve_time", row.getApproveTime());
		putLong(detail, "merchant_id", row.getMerchantId());
		detail.put("rule", row.getRule() == null ? "" : row.getRule());

		operatorDataPassExPassFormatter.applyExPassItemToMap(row, detail);

		long companyId = row.getCompanyId() != null ? row.getCompanyId() : 0L;
		int applicantOperatorId = row.getOperatorId() != null ? row.getOperatorId() : 0;
		Map<String, Object> base =
				operatorsQueryService.getInfo(
						Map.of("company_id", companyId, "operator_id", (long) applicantOperatorId));

		if (base == null || base.isEmpty()) {
			detail.put("operator_info", Collections.emptyList());
		} else {
			LinkedHashMap<String, Object> op = new LinkedHashMap<>(base);
			op.remove("password");
			decodeJsonColumnsIntoOperatorMap(op);
			enrichOperatorInfoForDataPassDetail(companyId, callerOperatorType, op);
			detail.put("operator_info", op);
		}

		return detail;
	}

	@SuppressWarnings("unchecked")
	private void enrichOperatorInfoForDataPassDetail(
			long companyId, String callerOperatorTypeNorm, LinkedHashMap<String, Object> operatorInfo) {
		operatorInfo.put("distributors", new ArrayList<Map<String, Object>>());

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
			if (id != null && id > 0L) {
				idSet.add(id);
			}
		}
		List<Long> idList = new ArrayList<>(idSet);

		String callerType = callerOperatorTypeNorm == null ? "" : callerOperatorTypeNorm;

		if (!idList.isEmpty()) {
			List<Distributor> entities = new ArrayList<>(distributorListQueryService.listByIdsAndCompany(companyId, idList));
			entities.sort(createdDescNullsLast());
			appendFormattedDistributors(companyId, entities, operatorInfo);
		} else if ("admin".equalsIgnoreCase(callerType)) {
			LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
			w.eq(Distributor::getCompanyId, companyId).orderByDesc(Distributor::getCreated).last("LIMIT 100");
			List<Distributor> entities = distributorMapper.selectList(w);
			appendFormattedDistributors(companyId, entities, operatorInfo);
		}

		if (hasZeroId || "admin".equalsIgnoreCase(callerType)) {
			Map<String, Object> selfDis =
					new LinkedHashMap<>(distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId));
			selfDis.put("is_center", Boolean.TRUE);
			Object distsObj = operatorInfo.get("distributors");
			if (distsObj instanceof List<?> listRaw) {
				List<Map<String, Object>> distributors = (List<Map<String, Object>>) listRaw;
				distributors.add(0, selfDis);
			}
		}

		Long opIdParsed = parseLongLoose(operatorInfo.get("operator_id"));
		long oid = opIdParsed != null ? opIdParsed : 0L;
		if (oid <= 0L) {
			operatorInfo.put("work_userid", "");
			operatorInfo.put("role_data", Collections.emptyList());
		} else {
			operatorInfo.put(
					"work_userid",
					distributorWorkWechatRelQueryService.findWorkUseridByCompanyAndOperator(companyId, oid));
			operatorInfo.put("role_data", operatorDeliveryStaffRoleDataService.getRoleDataList(companyId, oid));
		}
	}

	private void appendFormattedDistributors(long companyId, List<Distributor> entities, Map<String, Object> operatorInfo) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> distributors = (List<Map<String, Object>>) operatorInfo.get("distributors");
		for (Distributor d : entities) {
			long did = d.getDistributorId() != null ? d.getDistributorId() : 0L;
			int ds = d.getDistributorSelf() != null ? d.getDistributorSelf() : 0;
			Map<String, Object> setting = selfDeliverySettingReadService.getSetting(companyId, did, ds);
			Map<String, Object> rowMap = distributorListRowFormatService.formatStoreRow(d, setting, objectMapper);
			rowMap.put("is_center", Boolean.FALSE);
			distributors.add(rowMap);
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

	private static void putLong(Map<String, Object> m, String key, Long v) {
		m.put(key, v == null ? 0L : v);
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
}
