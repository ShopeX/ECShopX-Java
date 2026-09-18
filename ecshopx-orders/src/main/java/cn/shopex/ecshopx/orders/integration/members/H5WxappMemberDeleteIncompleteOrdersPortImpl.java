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

package cn.shopex.ecshopx.orders.integration.members;

import cn.shopex.ecshopx.common.members.h5.H5WxappMemberDeleteIncompleteOrdersPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("h5WxappMemberDeleteIncompleteOrdersPortImpl")
@RequiredArgsConstructor
public class H5WxappMemberDeleteIncompleteOrdersPortImpl implements H5WxappMemberDeleteIncompleteOrdersPort {

	private final NormalOrdersMapper normalOrdersMapper;

	@Override
	public boolean hasIncompleteNormalOrders(long companyId, long userId) {
		long cnt =
				normalOrdersMapper.selectCount(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getUserId, userId)
								.notIn(NormalOrders::getOrderStatus, List.of("DONE", "CANCEL")));
		return cnt > 0L;
	}
}
