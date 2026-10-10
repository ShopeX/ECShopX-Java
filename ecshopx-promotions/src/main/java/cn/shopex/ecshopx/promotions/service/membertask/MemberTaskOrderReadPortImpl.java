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

package cn.shopex.ecshopx.promotions.service.membertask;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskOrderSnapshot;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class MemberTaskOrderReadPortImpl implements MemberTaskOrderReadPort {

	private final NormalOrdersMapper normalOrdersMapper;

	public MemberTaskOrderReadPortImpl(NormalOrdersMapper normalOrdersMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
	}

	@Override
	public MemberTaskOrderSnapshot get(long companyId, long orderId) {
		NormalOrders row =
				normalOrdersMapper.selectOne(
						new QueryWrapper<NormalOrders>()
								.eq("company_id", companyId)
								.eq("order_id", orderId)
								.last("LIMIT 1"));
		if (row == null) {
			return null;
		}
		long totalFee = 0L;
		if (row.getTotalFee() != null) {
			try {
				totalFee = Long.parseLong(row.getTotalFee().trim());
			} catch (NumberFormatException ignored) {
				totalFee = 0L;
			}
		}
		return new MemberTaskOrderSnapshot(
				row.getCompanyId() == null ? 0L : row.getCompanyId(),
				row.getOrderId() == null ? 0L : row.getOrderId(),
				row.getUserId() == null ? 0L : row.getUserId(),
				row.getMemberTaskId() == null ? 0L : row.getMemberTaskId(),
				row.getOrderStatus(),
				row.getEndTime() == null ? 0L : row.getEndTime(),
				totalFee,
				row.getDistributorId() == null ? 0L : row.getDistributorId());
	}
}
