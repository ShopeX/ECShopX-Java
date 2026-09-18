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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.OrderEpidemicRegister;
import cn.shopex.ecshopx.orders.mapper.OrderEpidemicRegisterMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderEpidemicRegisterFrontDelService {

	private final OrderEpidemicRegisterMapper orderEpidemicRegisterMapper;

	public OrderEpidemicRegisterFrontDelService(OrderEpidemicRegisterMapper orderEpidemicRegisterMapper) {
		this.orderEpidemicRegisterMapper = orderEpidemicRegisterMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void delEpidemicRegister(String id) {
		String raw = id == null ? "" : id.trim();
		if (raw.isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}
		long pk;
		try {
			pk = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			throw new ResourceException("未查询到更新数据");
		}
		OrderEpidemicRegister existing = orderEpidemicRegisterMapper.selectById(pk);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}
		OrderEpidemicRegister patch = new OrderEpidemicRegister();
		patch.setId(pk);
		patch.setIsUse(0);
		int rows = orderEpidemicRegisterMapper.updateById(patch);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}
}
