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

package cn.shopex.ecshopx.orders.repository;

import cn.shopex.ecshopx.orders.domain.OrderEpidemicRegister;
import cn.shopex.ecshopx.orders.mapper.OrderEpidemicRegisterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class OrderEpidemicRegisterQueryRepository {

	private final OrderEpidemicRegisterMapper mapper;

	public OrderEpidemicRegisterQueryRepository(OrderEpidemicRegisterMapper mapper) {
		this.mapper = mapper;
	}

	public long countByFilter(OrderEpidemicRegisterListFilter f) {
		return mapper.selectCount(wrapper(f));
	}

	public List<OrderEpidemicRegister> pageByFilter(OrderEpidemicRegisterListFilter f, int page, int pageSize) {
		LambdaQueryWrapper<OrderEpidemicRegister> w = wrapper(f);
		w.select(OrderEpidemicRegister::getId, OrderEpidemicRegister::getOrderId, OrderEpidemicRegister::getUserId,
				OrderEpidemicRegister::getCompanyId, OrderEpidemicRegister::getDistributorId, OrderEpidemicRegister::getName,
				OrderEpidemicRegister::getMobile, OrderEpidemicRegister::getCreated);
		w.orderByDesc(OrderEpidemicRegister::getCreated);
		Page<OrderEpidemicRegister> p = new Page<>(page, pageSize, false);
		return mapper.selectPage(p, w).getRecords();
	}

	public List<OrderEpidemicRegister> pageByFilterForExport(OrderEpidemicRegisterListFilter f, int page, int pageSize) {
		LambdaQueryWrapper<OrderEpidemicRegister> w = wrapper(f);
		w.select(OrderEpidemicRegister::getId, OrderEpidemicRegister::getOrderId, OrderEpidemicRegister::getUserId,
				OrderEpidemicRegister::getCompanyId, OrderEpidemicRegister::getDistributorId, OrderEpidemicRegister::getName,
				OrderEpidemicRegister::getMobile, OrderEpidemicRegister::getCertId, OrderEpidemicRegister::getTemperature,
				OrderEpidemicRegister::getJob, OrderEpidemicRegister::getSymptom, OrderEpidemicRegister::getSymptomDes,
				OrderEpidemicRegister::getIsRiskArea, OrderEpidemicRegister::getCreated);
		w.orderByDesc(OrderEpidemicRegister::getCreated);
		Page<OrderEpidemicRegister> p = new Page<>(page, pageSize, false);
		return mapper.selectPage(p, w).getRecords();
	}

	public List<OrderEpidemicRegister> pageWxappEpidemicInfoFirstPage(
			OrderEpidemicRegisterListFilter f, int page, int pageSize) {
		LambdaQueryWrapper<OrderEpidemicRegister> w = wrapper(f);
		w.orderByDesc(OrderEpidemicRegister::getId);
		Page<OrderEpidemicRegister> p = new Page<>(page, pageSize, false);
		return mapper.selectPage(p, w).getRecords();
	}

	private static LambdaQueryWrapper<OrderEpidemicRegister> wrapper(OrderEpidemicRegisterListFilter f) {
		LambdaQueryWrapper<OrderEpidemicRegister> w = new LambdaQueryWrapper<>();
		w.eq(OrderEpidemicRegister::getCompanyId, (int) f.getCompanyId());
		if (f.getDistributorIdEq() != null) {
			w.eq(OrderEpidemicRegister::getDistributorId, f.getDistributorIdEq());
		} else if (f.getDistributorIdIn() != null && !f.getDistributorIdIn().isEmpty()) {
			w.in(OrderEpidemicRegister::getDistributorId, f.getDistributorIdIn());
		}
		if (f.getOrderTimeGte() != null) {
			w.ge(OrderEpidemicRegister::getOrderTime, f.getOrderTimeGte());
		}
		if (f.getOrderTimeLte() != null) {
			w.le(OrderEpidemicRegister::getOrderTime, f.getOrderTimeLte());
		}
		if (f.getUserIdEq() != null) {
			w.eq(OrderEpidemicRegister::getUserId, f.getUserIdEq());
		}
		if (f.getIsUseEq() != null) {
			w.eq(OrderEpidemicRegister::getIsUse, f.getIsUseEq());
		}
		return w;
	}
}
