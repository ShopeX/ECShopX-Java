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

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.orders.domain.OrderProcessLog;
import cn.shopex.ecshopx.orders.mapper.OrderProcessLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderProcessLogListService {

	private static final Pattern CN_MOBILE_11 = Pattern.compile("^1[3456789]\\d{9}$");

	private final OrderProcessLogMapper orderProcessLogMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final OperatorsQueryService operatorsQueryService;
	private final ObjectMapper objectMapper;

	public AdminOrderProcessLogListService(
			OrderProcessLogMapper orderProcessLogMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			OperatorsQueryService operatorsQueryService,
				ObjectMapper objectMapper) {
		this.orderProcessLogMapper = orderProcessLogMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.operatorsQueryService = operatorsQueryService;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> getOrderProcessLog(
			long companyId,
			String operatorType,
			long operatorIdJwt,
			String orderIdPath,
			boolean dataMaskBlock) {
		String raw = orderIdPath == null ? "" : orderIdPath.trim();

		LambdaQueryWrapper<OrderProcessLog> w = new LambdaQueryWrapper<>();
		w.apply("order_id = {0}", raw);
		w.eq(OrderProcessLog::getCompanyId, companyId);
		if ("supplier".equals(operatorType)) {
			w.in(OrderProcessLog::getSupplierId, List.of(0, clampSupplierOperatorId(operatorIdJwt)));
		}
		w.orderByDesc(OrderProcessLog::getCreateTime).orderByDesc(OrderProcessLog::getId);

		List<OrderProcessLog> rows = orderProcessLogMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return new ArrayList<>();
		}

		List<Map<String, Object>> out = new ArrayList<>();
		for (OrderProcessLog entity : rows) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("id", entity.getId());
			m.put("order_id", entity.getOrderId());
			m.put("company_id", entity.getCompanyId());
			m.put("supplier_id", entity.getSupplierId());
			m.put("operator_type", entity.getOperatorType());
			m.put("operator_id", entity.getOperatorId());

			String enc = entity.getOperatorName();
			String plain = enc == null || enc.isEmpty() ? enc : sensitiveFieldEncryptor.decrypt(enc);
			m.put("operator_name", plain);

			m.put("remarks", entity.getRemarks());
			m.put("detail", entity.getDetail());
			m.put("params", entity.getParams());
			m.put("is_show", entity.getIsShow());
			m.put("pics", decodePicsColumnToDisplay(entity.getPics()));
			m.put("delivery_remark", entity.getDeliveryRemark());
			m.put("create_time", entity.getCreateTime());
			m.put("update_time", entity.getUpdateTime());

			if ("admin".equals(entity.getOperatorType())
					&& entity.getOperatorId() != null
					&& entity.getOperatorId() > 0) {
				Map<String, Object> opFilter = new LinkedHashMap<>();
				opFilter.put("operator_id", entity.getOperatorId());
				Map<String, Object> operator = operatorsQueryService.getInfo(opFilter);
				if (operator != null && operator.get("mobile") != null) {
					m.put("operator_name", String.valueOf(operator.get("mobile")));
				}
			}

			if (dataMaskBlock) {
				Object nameObj = m.get("operator_name");
				if (nameObj != null) {
					String name = String.valueOf(nameObj);
					if (CN_MOBILE_11.matcher(name).matches()) {
						m.put("operator_name", DataMasking.maskMobile(name));
					}
				}
			}

			out.add(m);
		}
		return out;
	}

	private Object decodePicsColumnToDisplay(String picsColumn) {
		if (picsColumn == null) {
			return null;
		}
		String trimmed = picsColumn.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(trimmed, Object.class);
		} catch (Exception e) {
			return null;
		}
	}

	private static int clampSupplierOperatorId(long operatorIdJwt) {
		if (operatorIdJwt > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (operatorIdJwt < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) operatorIdJwt;
	}
}
