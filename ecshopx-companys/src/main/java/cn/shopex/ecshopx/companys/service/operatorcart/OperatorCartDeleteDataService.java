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

package cn.shopex.ecshopx.companys.service.operatorcart;

import cn.shopex.ecshopx.companys.domain.OperatorCart;
import cn.shopex.ecshopx.companys.mapper.OperatorCartMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class OperatorCartDeleteDataService {

	private final OperatorCartMapper operatorCartMapper;

	public OperatorCartDeleteDataService(OperatorCartMapper operatorCartMapper) {
		this.operatorCartMapper = operatorCartMapper;
	}

	public boolean delCartData(long companyId, long operatorId, Long cartIdFilter, Long itemIdFilter) {
		LambdaQueryWrapper<OperatorCart> w = new LambdaQueryWrapper<>();
		w.eq(OperatorCart::getCompanyId, companyId).eq(OperatorCart::getOperatorId, operatorId);
		if (cartIdFilter != null) {
			w.eq(OperatorCart::getCartId, cartIdFilter);
		}
		if (itemIdFilter != null) {
			w.eq(OperatorCart::getItemId, itemIdFilter);
		}
		operatorCartMapper.delete(w);
		return true;
	}
}
