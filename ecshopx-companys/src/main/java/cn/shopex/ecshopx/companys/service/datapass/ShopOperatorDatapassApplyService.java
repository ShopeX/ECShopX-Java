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

package cn.shopex.ecshopx.companys.service.datapass;

import cn.shopex.ecshopx.companys.domain.OperatorDataPassLog;
import cn.shopex.ecshopx.companys.mapper.OperatorDataPassLogMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopOperatorDatapassApplyService {

	private final DeliveryStaffBypassForDatapassService deliveryStaffBypassForDatapassService;
	private final OperatorDataPassCheckService operatorDataPassCheckService;
	private final OperatorDataPassLogMapper operatorDataPassLogMapper;
	private final OperatorsQueryService operatorsQueryService;

	public ShopOperatorDatapassApplyService(
			DeliveryStaffBypassForDatapassService deliveryStaffBypassForDatapassService,
			OperatorDataPassCheckService operatorDataPassCheckService,
			OperatorDataPassLogMapper operatorDataPassLogMapper,
			OperatorsQueryService operatorsQueryService) {
		this.deliveryStaffBypassForDatapassService = deliveryStaffBypassForDatapassService;
		this.operatorDataPassCheckService = operatorDataPassCheckService;
		this.operatorDataPassLogMapper = operatorDataPassLogMapper;
		this.operatorsQueryService = operatorsQueryService;
	}

	public void apply(HttpServletRequest request, Map<String, Object> user, String pathAlias) {
		long companyId = longOf(user.get("company_id"));
		long operatorId = longOf(user.get("operator_id"));
		String mobile = stringOf(user.get("mobile"));
		String operatorType = stringOf(user.get("operator_type"));

		if ("user".equals(operatorType) && StringUtils.hasText(mobile)) {
			if (deliveryStaffBypassForDatapassService.existsSelfDeliveryStaffForMobile(companyId, mobile)) {
				return;
			}
		}
		if ("admin".equals(operatorType)) {
			return;
		}
		if ("merchant".equals(operatorType) && isMerchantMain(user, companyId, operatorId)) {
			return;
		}
		if (!operatorDataPassCheckService.check(companyId, operatorId)) {
			request.setAttribute("x-datapass-block", 1);
			return;
		}
		OperatorDataPassLog log = new OperatorDataPassLog();
		log.setCompanyId(companyId);
		log.setOperatorId((int) operatorId);
		log.setCreateTime((int) (System.currentTimeMillis() / 1000L));
		log.setPath(pathAlias);
		log.setUrl(fullUrl(request));
		operatorDataPassLogMapper.insert(log);
	}

	private boolean isMerchantMain(Map<String, Object> user, long companyId, long operatorId) {
		if (isMerchantMainTruthy(user.get("is_merchant_main"))) {
			return true;
		}
		if (isMerchantMainTruthy(user.get("isMerchantMain"))) {
			return true;
		}
		if (operatorId <= 0L || companyId <= 0L) {
			return false;
		}
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("operator_id", operatorId);
		Map<String, Object> op = operatorsQueryService.getInfo(filter);
		if (op == null || op.isEmpty()) {
			return false;
		}
		return isMerchantMainTruthy(op.get("is_merchant_main"))
				|| isMerchantMainTruthy(op.get("isMerchantMain"));
	}

	private static boolean isMerchantMainTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String t = raw.toString().trim();
		if (!StringUtils.hasText(t)) {
			return false;
		}
		return "1".equals(t) || "true".equalsIgnoreCase(t);
	}

	private static String fullUrl(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder(request.getRequestURL());
		String q = request.getQueryString();
		if (q != null) {
			sb.append('?').append(q);
		}
		return sb.toString();
	}

	private static long longOf(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return o == null ? 0L : Long.parseLong(o.toString());
	}

	private static String stringOf(Object o) {
		return o == null ? "" : o.toString();
	}
}
