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

package cn.shopex.ecshopx.companys.integration.members;

import cn.shopex.ecshopx.common.members.port.WxappMemberSelfDeliveryStaffListPort;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service("wxappMemberSelfDeliveryStaffListPortImpl")
public class WxappMemberSelfDeliveryStaffListPortImpl implements WxappMemberSelfDeliveryStaffListPort {

	private final OperatorsMapper operatorsMapper;

	public WxappMemberSelfDeliveryStaffListPortImpl(OperatorsMapper operatorsMapper) {
		this.operatorsMapper = operatorsMapper;
	}

	@Override
	public List<Map<String, Object>> listByPlainMobile(long companyId, String plainMobile) {
		if (plainMobile == null || plainMobile.isBlank()) {
			return List.of();
		}
		List<Operators> rows =
				operatorsMapper.selectList(
						new LambdaQueryWrapper<Operators>()
								.eq(Operators::getCompanyId, companyId)
								.eq(Operators::getMobile, plainMobile.trim())
								.eq(Operators::getOperatorType, "self_delivery_staff"));
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Operators op : rows) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("operator_id", op.getOperatorId());
			m.put("username", op.getUsername());
			m.put("mobile", op.getMobile());
			m.put("operator_type", op.getOperatorType());
			out.add(m);
		}
		return out;
	}
}
