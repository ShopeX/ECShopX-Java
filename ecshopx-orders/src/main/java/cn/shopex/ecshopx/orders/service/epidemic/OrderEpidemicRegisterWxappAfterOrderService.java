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

package cn.shopex.ecshopx.orders.service.epidemic;

import cn.shopex.ecshopx.orders.domain.OrderEpidemicRegister;
import cn.shopex.ecshopx.orders.mapper.OrderEpidemicRegisterMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OrderEpidemicRegisterWxappAfterOrderService {

	private final OrderEpidemicRegisterMapper orderEpidemicRegisterMapper;

	public OrderEpidemicRegisterWxappAfterOrderService(OrderEpidemicRegisterMapper orderEpidemicRegisterMapper) {
		this.orderEpidemicRegisterMapper = orderEpidemicRegisterMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void run(Map<String, Object> epidemicInfo, Map<String, Object> orderCreateResult) {
		if (epidemicInfo == null || orderCreateResult == null) {
			return;
		}
		long orderId = longVal(orderCreateResult.get("order_id"));
		long userId = longVal(orderCreateResult.get("user_id"));
		int companyId = (int) Math.min(longVal(orderCreateResult.get("company_id")), Integer.MAX_VALUE);
		long distributorId = longVal(orderCreateResult.get("distributor_id"));
		String certId = stringVal(epidemicInfo.get("cert_id"));
		if (StringUtils.hasText(certId)) {
			orderEpidemicRegisterMapper.update(
					null,
					new LambdaUpdateWrapper<OrderEpidemicRegister>()
							.eq(OrderEpidemicRegister::getCompanyId, companyId)
							.eq(OrderEpidemicRegister::getCertId, certId)
							.set(OrderEpidemicRegister::getIsUse, 0));
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		OrderEpidemicRegister row = new OrderEpidemicRegister();
		row.setOrderId(orderId);
		row.setUserId(userId);
		row.setCompanyId(companyId);
		row.setDistributorId(distributorId);
		row.setName(stringVal(epidemicInfo.get("name")));
		row.setMobile(stringVal(epidemicInfo.get("mobile")));
		row.setCertId(certId);
		row.setTemperature(stringVal(epidemicInfo.get("temperature")));
		row.setJob(stringVal(epidemicInfo.get("job")));
		row.setSymptom(stringVal(epidemicInfo.get("symptom")));
		row.setSymptomDes(stringVal(epidemicInfo.get("symptom_des")));
		row.setIsRiskArea(intVal(epidemicInfo.get("is_risk_area"), 0));
		row.setIsUse(1);
		row.setOrderTime(now);
		row.setCreated(now);
		row.setUpdated(now);
		orderEpidemicRegisterMapper.insert(row);
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
