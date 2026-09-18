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

package cn.shopex.ecshopx.orders.service.reservation;

import cn.shopex.ecshopx.orders.domain.Rights;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import cn.shopex.ecshopx.reservation.port.ReservationWxappCanRightsListPort;
import cn.shopex.ecshopx.reservation.port.ReservationWxappCanRightsListPort.WxappCanRightsListResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrdersReservationWxappCanRightsListPort implements ReservationWxappCanRightsListPort {

	private final RightsMapper rightsMapper;

	private final RightsTimesCardRowFactory rightsTimesCardRowFactory;

	public OrdersReservationWxappCanRightsListPort(
			RightsMapper rightsMapper, RightsTimesCardRowFactory rightsTimesCardRowFactory) {
		this.rightsMapper = rightsMapper;
		this.rightsTimesCardRowFactory = rightsTimesCardRowFactory;
	}

	@Override
	public WxappCanRightsListResult queryPage(
			long companyId, Long userId, int nowEpochSec, int page, int pageSize) {
		int p = page < 1 ? 1 : page;
		int ps = pageSize;
		if (ps > 1000) {
			ps = 1000;
		}
		if (ps <= 0) {
			ps = 20;
		}

		LambdaQueryWrapper<Rights> w = Wrappers.lambdaQuery();
		w.eq(Rights::getCompanyId, companyId);
		if (userId != null) {
			w.eq(Rights::getUserId, userId);
		}
		w.eq(Rights::getStatus, "valid");
		w.le(Rights::getStartTime, nowEpochSec);
		w.gt(Rights::getEndTime, nowEpochSec);
		w.orderByAsc(Rights::getEndTime);

		Page<Rights> mpPage = new Page<>(p, ps);
		Page<Rights> result = rightsMapper.selectPage(mpPage, w);

		List<Map<String, Object>> rows = new ArrayList<>();
		for (Rights r : result.getRecords()) {
			rows.add(rightsTimesCardRowFactory.toTimesCardRow(r, nowEpochSec));
		}
		return new WxappCanRightsListResult(rows, result.getTotal());
	}
}
