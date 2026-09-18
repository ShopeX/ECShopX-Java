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

package cn.shopex.ecshopx.companys.service.employee;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.dto.SelfDeliveryStaffAccountListRow;
import cn.shopex.ecshopx.companys.mapper.EmployeeSelfDeliveryStaffMapper;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.deliverystaff.OperatorDeliveryStaffRoleDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeeManagementDetailService {

	private final OperatorsMapper operatorsMapper;
	private final OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService;
	private final EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper;
	private final OperatorAccountOutsideLangReadService operatorAccountOutsideLangReadService;
	private final ObjectMapper objectMapper;

	public EmployeeManagementDetailService(
			OperatorsMapper operatorsMapper,
			OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService,
			EmployeeSelfDeliveryStaffMapper employeeSelfDeliveryStaffMapper,
			OperatorAccountOutsideLangReadService operatorAccountOutsideLangReadService,
			ObjectMapper objectMapper) {
		this.operatorsMapper = operatorsMapper;
		this.operatorDeliveryStaffRoleDataService = operatorDeliveryStaffRoleDataService;
		this.employeeSelfDeliveryStaffMapper = employeeSelfDeliveryStaffMapper;
		this.operatorAccountOutsideLangReadService = operatorAccountOutsideLangReadService;
		this.objectMapper = objectMapper;
	}

	@Nullable
	public Map<String, Object> getInfoData(
			Map<String, Object> jwt, long targetOperatorId, String requestLangTag) {
		Long companyId = EmployeeOrchestrationSupport.longClaimOrNull(jwt, "company_id", "companyId");
		if (companyId == null) {
			throw new BadRequestException("参数 company_id 错误");
		}

		LambdaQueryWrapper<Operators> w = new LambdaQueryWrapper<>();
		w.eq(Operators::getCompanyId, companyId).eq(Operators::getOperatorId, targetOperatorId);
		Operators op = operatorsMapper.selectOne(w);
		if (op == null) {
			return null;
		}

		Map<String, Object> row = toRow(op);

		List<Map<String, Object>> roleData =
				operatorDeliveryStaffRoleDataService.getRoleDataList(companyId, targetOperatorId);
		row.put("role_data", roleData != null ? roleData : List.of());

		if ("self_delivery_staff".equals(op.getOperatorType())) {
			List<SelfDeliveryStaffAccountListRow> sRows =
					employeeSelfDeliveryStaffMapper.selectAccountListRowsByCompanyIdAndOperatorIds(
							companyId, List.of(targetOperatorId));
			if (sRows != null && !sRows.isEmpty()) {
				mergeSelfDelivery(row, sRows.get(0));
			}
		}

		operatorAccountOutsideLangReadService.applyForAccountList(companyId, requestLangTag, List.of(row));
		return row;
	}

	private Map<String, Object> toRow(Operators op) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("operator_id", op.getOperatorId());
		m.put("mobile", op.getMobile());
		m.put("login_name", op.getLoginName());
		m.put("password", op.getPassword());
		m.put("eid", op.getEid());
		m.put("passport_uid", op.getPassportUid());
		m.put("operator_type", op.getOperatorType());
		m.put("shop_ids", detailShopIdsForResponse(op.getShopIds()));
		m.put("distributor_ids", detailDistributorIdsForResponse(op.getDistributorIds()));
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

	private void mergeSelfDelivery(Map<String, Object> row, SelfDeliveryStaffAccountListRow sd) {
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

	@Nullable
	private Object detailShopIdsForResponse(@Nullable String rawFromEntity) {
		if (rawFromEntity == null) {
			return null;
		}
		String raw = rawFromEntity.trim();
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			JsonNode node = objectMapper.readTree(raw);
			if (!node.isArray()) {
				return null;
			}
			if (node.isEmpty()) {
				return List.of();
			}
			return objectMapper.convertValue(node, new TypeReference<List<Object>>() {});
		} catch (Exception e) {
			return null;
		}
	}

	@Nullable
	private Object detailDistributorIdsForResponse(@Nullable String rawFromEntity) {
		if (rawFromEntity == null) {
			return null;
		}
		String raw = rawFromEntity.trim();
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			JsonNode node = objectMapper.readTree(raw);
			if (!node.isArray()) {
				return null;
			}
			if (node.isEmpty()) {
				return List.of();
			}
			return objectMapper.convertValue(node, new TypeReference<List<Map<String, Object>>>() {});
		} catch (Exception e) {
			return null;
		}
	}
}
