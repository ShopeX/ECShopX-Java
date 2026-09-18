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

package cn.shopex.ecshopx.datacube.service.deliverystaff;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.repository.OperatorsDeliveryStaffDataSupportRepository;
import cn.shopex.ecshopx.companys.service.deliverystaff.AdminDeliveryStaffDataExportFilter;
import cn.shopex.ecshopx.companys.service.deliverystaff.OperatorDeliveryStaffRoleDataService;
import cn.shopex.ecshopx.distribution.domain.SelfDeliveryStaff;
import cn.shopex.ecshopx.distribution.mapper.SelfDeliveryStaffMapper;
import cn.shopex.ecshopx.orders.domain.dto.DeliveryStaffOrderAggregateRow;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminDeliveryStaffDataListService {

	private final OperatorsDeliveryStaffDataSupportRepository operatorsDeliveryStaffDataSupportRepository;

	private final SelfDeliveryStaffMapper selfDeliveryStaffMapper;

	private final NormalOrdersMapper normalOrdersMapper;

	private final ObjectMapper objectMapper;

	private final OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService;

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public AdminDeliveryStaffDataListService(
			OperatorsDeliveryStaffDataSupportRepository operatorsDeliveryStaffDataSupportRepository,
			SelfDeliveryStaffMapper selfDeliveryStaffMapper,
			NormalOrdersMapper normalOrdersMapper,
			ObjectMapper objectMapper,
			OperatorDeliveryStaffRoleDataService operatorDeliveryStaffRoleDataService,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.operatorsDeliveryStaffDataSupportRepository = operatorsDeliveryStaffDataSupportRepository;
		this.selfDeliveryStaffMapper = selfDeliveryStaffMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.objectMapper = objectMapper;
		this.operatorDeliveryStaffRoleDataService = operatorDeliveryStaffRoleDataService;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> getDeliveryStaffDataList(AdminDeliveryStaffDataExportFilter filter, int page, int pageSize) {
		if (filter.getCompanyId() <= 0) {
			throw new BadRequestException("参数company_id错误");
		}
		long total = operatorsDeliveryStaffDataSupportRepository.count(filter);
		if (total == 0) {
			return Map.of("total_count", 0L, "list", List.of());
		}
		List<Operators> operators =
				operatorsDeliveryStaffDataSupportRepository.page(filter, page, pageSize);
		if (operators.isEmpty()) {
			return Map.of("total_count", total, "list", List.of());
		}
		Map<Long, List<Map<String, Object>>> roleByOperator =
				operatorDeliveryStaffRoleDataService.roleDataRowsForOperators(filter.getCompanyId(), operators);
		List<Long> operatorIds =
				operators.stream().map(Operators::getOperatorId).filter(Objects::nonNull).toList();
		List<SelfDeliveryStaff> extRows = selfDeliveryStaffMapper.selectByOperatorIds(operatorIds);
		Map<Long, SelfDeliveryStaff> extByOp = extRows.stream()
				.filter(r -> r.getOperatorId() != null)
				.collect(Collectors.toMap(SelfDeliveryStaff::getOperatorId, r -> r, (a, b) -> a));

		List<Long> distributorIdsForOrders;
		if (filter.getDistributorId() != null && filter.getDistributorId() > 0) {
			distributorIdsForOrders = List.of(filter.getDistributorId());
		} else {
			distributorIdsForOrders = List.of();
		}
		List<DeliveryStaffOrderAggregateRow> aggRows = normalOrdersMapper.selectDeliveryStaffAggregates(
				filter.getCompanyId(),
				filter.getStartEpoch(),
				filter.getEndEpoch(),
				operatorIds,
				distributorIdsForOrders,
				filter.getMerchantIdForOperators());
		Map<Long, DeliveryStaffOrderAggregateRow> aggByOp = new LinkedHashMap<>();
		for (DeliveryStaffOrderAggregateRow r : aggRows) {
			if (r.getSelfDeliveryOperatorId() != null) {
				aggByOp.put(r.getSelfDeliveryOperatorId(), r);
			}
		}

		List<Map<String, Object>> list = new ArrayList<>();
		for (Operators op : operators) {
			Map<String, Object> row = new LinkedHashMap<>();
			putOperatorColumns(op, row);

			Long oid = op.getOperatorId();
			row.put("role_data", roleByOperator.getOrDefault(oid, List.of()));

			SelfDeliveryStaff s = oid != null ? extByOp.get(oid) : null;
			if (s != null) {
				row.put("staff_no", s.getStaffNo());
				row.put("payment_method", s.getPaymentMethod());
				row.put("payment_fee", s.getPaymentFee());
				row.put("staff_type", s.getStaffType());
				row.put("staff_attribute", s.getStaffAttribute());
			} else {
				row.put("staff_no", null);
				row.put("payment_method", null);
				row.put("payment_fee", null);
				row.put("staff_type", null);
				row.put("staff_attribute", null);
			}

			DeliveryStaffOrderAggregateRow a = oid != null ? aggByOp.get(oid) : null;
			row.put("user_count", a != null && a.getUserCount() != null ? a.getUserCount() : 0L);
			row.put("order_count", a != null && a.getOrderCount() != null ? a.getOrderCount() : 0L);
			row.put("total_fee_count", feeToLong(a != null ? a.getTotalFeeCount() : null));
			row.put("self_delivery_fee_count", feeToLong(a != null ? a.getSelfDeliveryFeeCount() : null));
			list.add(row);
		}
		return Map.of("total_count", total, "list", list);
	}

	private void putOperatorColumns(Operators op, Map<String, Object> row) {
		row.put("operator_id", op.getOperatorId());
		row.put("company_id", op.getCompanyId());
		row.put("mobile", sensitiveFieldEncryptor.decrypt(op.getMobile()));
		row.put("login_name", op.getLoginName());
		row.put("operator_type", op.getOperatorType());
		row.put("password", op.getPassword());
		row.put("eid", op.getEid());
		row.put("passport_uid", op.getPassportUid());
		row.put("created", op.getCreated());
		row.put("updated", op.getUpdated());
		row.put("distributor_ids", parseDistributorIdsJson(op.getDistributorIds()));
		row.put("shop_ids", op.getShopIds());
		row.put("username", op.getUsername());
		row.put("head_portrait", op.getHeadPortrait());
		row.put("regionauth_id", op.getRegionauthId());
		row.put("contact", op.getContact());
		row.put("split_ledger_info", op.getSplitLedgerInfo());
		row.put("is_disable", op.getIsDisable());
		row.put("adapay_open_account_time", op.getAdapayOpenAccountTime());
		row.put("dealer_parent_id", op.getDealerParentId());
		row.put("is_dealer_main", op.getIsDealerMain());
		row.put("merchant_id", op.getMerchantId());
		row.put("is_merchant_main", op.getIsMerchantMain());
		row.put("is_distributor_main", op.getIsDistributorMain());
	}

	private List<Map<String, Object>> parseDistributorIdsJson(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			List<Map<String, Object>> parsed =
					objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
			return parsed != null ? parsed : List.of();
		} catch (Exception e) {
			return List.of();
		}
	}

	private static long feeToLong(BigDecimal v) {
		if (v == null) {
			return 0L;
		}
		return v.longValue();
	}
}
