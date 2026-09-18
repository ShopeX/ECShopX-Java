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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.dto.SelfDeliveryStaffAccountListRow;
import cn.shopex.ecshopx.companys.mapper.EmployeeSelfDeliveryStaffMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.companys.service.employee.AccountManagementOperatorsFilter;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.repository.DistributorAdminListFilter;
import cn.shopex.ecshopx.distribution.repository.DistributorAdminListRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SelfDeliveryStaffDistributorService {

	private final OperatorsQueryService operatorsQueryService;
	private final EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper;
	private final DistributorAdminListRepository distributorAdminListRepository;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorListOutsideLangReadService distributorListOutsideLangReadService;
	private final ObjectMapper objectMapper;

	public SelfDeliveryStaffDistributorService(
			OperatorsQueryService operatorsQueryService,
			EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper,
			DistributorAdminListRepository distributorAdminListRepository,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorListOutsideLangReadService distributorListOutsideLangReadService,
			ObjectMapper objectMapper) {
		this.operatorsQueryService = operatorsQueryService;
		this.employeeSelfDeliveryStaffMapper = employeeSelfDeliveryStaffMapper;
		this.distributorAdminListRepository = distributorAdminListRepository;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorListOutsideLangReadService = distributorListOutsideLangReadService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getSelfDeliveryStaffDistributor(
			long companyId, List<Long> selfDeliveryOperatorIds, String requestLangTag) {
		if (companyId <= 0L) {
			throw new BadRequestException("参数company_id错误");
		}

		AccountManagementOperatorsFilter f = new AccountManagementOperatorsFilter();
		f.setCompanyId(companyId);
		f.setOperatorIds(selfDeliveryOperatorIds);
		f.setOperatorType("self_delivery_staff");
		List<AccountManagementOperatorsFilter.OrderBy> orderBy =
				List.of(new AccountManagementOperatorsFilter.OrderBy(
						"created", AccountManagementOperatorsFilter.OrderBy.Direction.DESC));

		int pageSize = 500;
		int offset = 0;
		long total = operatorsQueryService.countAccountManagementList(f);
		List<Operators> operatorsRows =
				total > 0L ? operatorsQueryService.pageAccountManagementList(f, offset, pageSize, orderBy) : List.of();

		Map<Long, SelfDeliveryStaffAccountListRow> selfByOp = new LinkedHashMap<>();
		if ("self_delivery_staff".equals(f.getOperatorType()) && total > 0L && !operatorsRows.isEmpty()) {
			List<Long> opIds = operatorsRows.stream()
					.map(Operators::getOperatorId)
					.filter(Objects::nonNull)
					.toList();
			List<SelfDeliveryStaffAccountListRow> sRows =
					employeeSelfDeliveryStaffMapper.selectAccountListRowsByCompanyIdAndOperatorIds(companyId, opIds);
			for (SelfDeliveryStaffAccountListRow r : sRows) {
				if (r.getOperatorId() != null) {
					selfByOp.put(r.getOperatorId(), r);
				}
			}
		}

		List<Map<String, Object>> mergedRows = new ArrayList<>();
		for (Operators op : operatorsRows) {
			Map<String, Object> map = operatorToMap(op);
			Long oid = op.getOperatorId();
			if (oid != null) {
				SelfDeliveryStaffAccountListRow sd = selfByOp.get(oid);
				if (sd != null) {
					mergeSelfDeliveryStaffIntoOperatorRow(map, sd);
				}
			}
			mergedRows.add(map);
		}

		LinkedHashSet<Long> idSet = new LinkedHashSet<>();
		for (Map<String, Object> row : mergedRows) {
			collectDistributorIdsInto(row, idSet);
		}

		if (idSet.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("list", List.of());
			empty.put("total_count", 0L);
			return empty;
		}

		DistributorAdminListFilter df = new DistributorAdminListFilter();
		df.setCompanyId(companyId);
		df.setDistributorIdIn(new ArrayList<>(idSet));
		df.setRequestLang(
				requestLangTag != null && !requestLangTag.isBlank() ? requestLangTag.trim() : "zh-CN");
		df.setOffset(0);
		df.setLimit(100);

		long distTotal = distributorAdminListRepository.countByFilter(df);
		List<Distributor> entities =
				distTotal > 0L ? distributorAdminListRepository.selectPageByFilter(df) : List.of();

		List<Map<String, Object>> rowMapsList = new ArrayList<>();
		for (Distributor entity : entities) {
			if (entity.getMobile() != null) {
				entity.setMobile(sensitiveFieldEncryptor.decrypt(entity.getMobile()));
			}
			if (entity.getContact() != null) {
				entity.setContact(sensitiveFieldEncryptor.decrypt(entity.getContact()));
			}
			Map<String, Object> row = new LinkedHashMap<>(DistributorRowMaps.toApiRow(entity, objectMapper));
			rowMapsList.add(row);
		}
		distributorListOutsideLangReadService.applyLangMaps(companyId, df.getRequestLang(), rowMapsList);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", rowMapsList);
		out.put("total_count", distTotal);
		return out;
	}

	private void collectDistributorIdsInto(Map<String, Object> row, LinkedHashSet<Long> idSet) {
		Object rawList = row.get("distributor_ids");
		if (rawList == null || !(rawList instanceof List<?>) || ((List<?>) rawList).isEmpty()) {
			return;
		}
		for (Object o : (List<?>) rawList) {
			if (!(o instanceof Map<?, ?> m)) {
				continue;
			}
			long v = parsePositiveDistributorId(m.get("distributor_id"));
			if (v > 0L) {
				idSet.add(v);
			}
		}
	}

	private static long parsePositiveDistributorId(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : 0L;
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			try {
				long v = Long.parseLong(s.trim());
				return v > 0L ? v : 0L;
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			long v = Long.parseLong(String.valueOf(raw).trim());
			return v > 0L ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private void mergeSelfDeliveryStaffIntoOperatorRow(Map<String, Object> row, SelfDeliveryStaffAccountListRow sd) {
		if (sd.getDistributorId() != null) {
			row.put("distributor_id", sd.getDistributorId());
		}
		if (sd.getShopId() != null) {
			row.put("shop_id", sd.getShopId());
		}
		if (sd.getStaffAttribute() != null) {
			row.put("staff_attribute", sd.getStaffAttribute());
		}
		if (sd.getStaffNo() != null) {
			row.put("staff_no", sd.getStaffNo());
		}
		if (sd.getStaffType() != null) {
			row.put("staff_type", sd.getStaffType());
		}
		if (sd.getPaymentMethod() != null) {
			row.put("payment_method", sd.getPaymentMethod());
		}
		if (sd.getPaymentFee() != null) {
			row.put("payment_fee", sd.getPaymentFee());
		}
		if (sd.getCreated() != null) {
			row.put("created", sd.getCreated());
		}
		if (sd.getUpdated() != null) {
			row.put("updated", sd.getUpdated());
		}
	}

	private Map<String, Object> operatorToMap(Operators op) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("operator_id", op.getOperatorId());
		m.put("mobile", op.getMobile());
		m.put("login_name", op.getLoginName());
		m.put("password", op.getPassword());
		m.put("eid", op.getEid());
		m.put("passport_uid", op.getPassportUid());
		m.put("operator_type", op.getOperatorType());
		m.put("shop_ids", parseJsonArray(op.getShopIds()));
		m.put("distributor_ids", parseDistributorIdsForResponse(op.getDistributorIds()));
		m.put("company_id", op.getCompanyId());
		m.put("username", op.getUsername());
		m.put("head_portrait", op.getHeadPortrait());
		m.put("regionauth_id", op.getRegionauthId());
		m.put("split_ledger_info", op.getSplitLedgerInfo());
		m.put("contact", op.getContact());
		m.put("is_disable", op.getIsDisable());
		m.put("adapay_open_account_time", op.getAdapayOpenAccountTime());
		m.put("dealer_parent_id", op.getDealerParentId());
		m.put("is_dealer_main", op.getIsDealerMain());
		m.put("created", op.getCreated());
		m.put("updated", op.getUpdated());
		m.put("merchant_id", op.getMerchantId());
		m.put("is_merchant_main", op.getIsMerchantMain());
		m.put("is_distributor_main", op.getIsDistributorMain());
		return m;
	}

	private List<Object> parseJsonArray(String json) {
		if (!StringUtils.hasText(json)) {
			return new ArrayList<>();
		}
		try {
			List<Object> parsed = objectMapper.readValue(json, new TypeReference<List<Object>>() {});
			return parsed != null ? parsed : new ArrayList<>();
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private List<Map<String, Object>> parseDistributorIdsForResponse(String json) {
		if (!StringUtils.hasText(json)) {
			return new ArrayList<>();
		}
		try {
			List<Map<String, Object>> parsed =
					objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
			return parsed != null ? parsed : new ArrayList<>();
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}
}
