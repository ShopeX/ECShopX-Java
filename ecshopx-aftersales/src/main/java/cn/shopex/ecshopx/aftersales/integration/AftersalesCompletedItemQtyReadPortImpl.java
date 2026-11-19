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

package cn.shopex.ecshopx.aftersales.integration;

import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesCompletedItemQtyReadPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AftersalesCompletedItemQtyReadPortImpl implements AftersalesCompletedItemQtyReadPort {

	private final AftersalesDetailMapper aftersalesDetailMapper;

	public AftersalesCompletedItemQtyReadPortImpl(AftersalesDetailMapper aftersalesDetailMapper) {
		this.aftersalesDetailMapper = aftersalesDetailMapper;
	}

	@Override
	public int sumCompletedNum(long companyId, long orderId) {
		if (companyId <= 0L || orderId <= 0L) {
			return 0;
		}
		List<AftersalesDetail> rows =
				aftersalesDetailMapper.selectList(
						new LambdaQueryWrapper<AftersalesDetail>()
								.eq(AftersalesDetail::getCompanyId, companyId)
								.eq(AftersalesDetail::getOrderId, String.valueOf(orderId))
								.eq(AftersalesDetail::getAftersalesStatus, 2)
								.select(AftersalesDetail::getNum));
		if (rows == null || rows.isEmpty()) {
			return 0;
		}
		int sum = 0;
		for (AftersalesDetail row : rows) {
			if (row != null && row.getNum() != null) {
				sum += row.getNum();
			}
		}
		return sum;
	}
}
