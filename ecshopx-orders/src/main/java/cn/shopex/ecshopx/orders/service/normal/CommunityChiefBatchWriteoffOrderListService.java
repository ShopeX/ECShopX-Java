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

package cn.shopex.ecshopx.orders.service.normal;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CommunityChiefBatchWriteoffOrderListService {

	private final NormalOrdersMapper normalOrdersMapper;

	public CommunityChiefBatchWriteoffOrderListService(NormalOrdersMapper normalOrdersMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
	}

	public List<NormalOrders> listPendingCommunityOrdersForChiefBatchWriteoff(long companyId, long actId) {
		LambdaQueryWrapper<NormalOrders> w = new LambdaQueryWrapper<>();
		w.eq(NormalOrders::getCompanyId, companyId)
				.eq(NormalOrders::getActId, actId)
				.eq(NormalOrders::getPayStatus, "PAYED")
				.in(NormalOrders::getCancelStatus, "NO_APPLY_CANCEL", "FAILS")
				.eq(NormalOrders::getZitiStatus, "PENDING")
				.eq(NormalOrders::getOrderClass, "community")
				.orderByAsc(NormalOrders::getOrderId);
		return normalOrdersMapper.selectList(w);
	}
}
