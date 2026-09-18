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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.orders.domain.OrderEpidemicRegister;
import cn.shopex.ecshopx.orders.repository.OrderEpidemicRegisterListFilter;
import cn.shopex.ecshopx.orders.repository.OrderEpidemicRegisterQueryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappEpidemicRegisterInfoService {

	private final OrderEpidemicRegisterQueryRepository orderEpidemicRegisterQueryRepository;

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public WxappEpidemicRegisterInfoService(
			OrderEpidemicRegisterQueryRepository orderEpidemicRegisterQueryRepository,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.orderEpidemicRegisterQueryRepository = orderEpidemicRegisterQueryRepository;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<String, Object> epidemicRegisterInfo(long companyId, long userId) {
		OrderEpidemicRegisterListFilter f = new OrderEpidemicRegisterListFilter();
		f.setCompanyId(companyId);
		f.setUserIdEq(userId);
		f.setIsUseEq(1);
		long total = orderEpidemicRegisterQueryRepository.countByFilter(f);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		if (total == 0L) {
			out.put("list", List.of());
			return out;
		}
		List<OrderEpidemicRegister> rows = orderEpidemicRegisterQueryRepository.pageWxappEpidemicInfoFirstPage(f, 1, 5);
		List<Map<String, Object>> listMaps = new ArrayList<>(rows.size());
		for (OrderEpidemicRegister r : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", r.getId());
			m.put("order_id", r.getOrderId());
			m.put("user_id", r.getUserId());
			m.put("company_id", r.getCompanyId());
			m.put("distributor_id", r.getDistributorId());
			m.put("name", sensitiveFieldEncryptor.decrypt(r.getName()));
			m.put("mobile", sensitiveFieldEncryptor.decrypt(r.getMobile()));
			m.put("cert_id", sensitiveFieldEncryptor.decrypt(r.getCertId()));
			m.put("temperature", r.getTemperature());
			m.put("job", r.getJob());
			m.put("symptom", r.getSymptom());
			m.put("symptom_des", r.getSymptomDes());
			m.put("is_risk_area", r.getIsRiskArea());
			m.put("is_use", r.getIsUse());
			m.put("order_time", r.getOrderTime());
			m.put("created", r.getCreated());
			m.put("updated", r.getUpdated());
			listMaps.add(m);
		}
		out.put("list", listMaps);
		return out;
	}
}
