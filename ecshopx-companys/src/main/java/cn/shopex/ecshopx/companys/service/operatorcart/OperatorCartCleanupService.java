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
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

/** 商品删除或列表懒清理时移除 {@code operator_cart} 行。 */
@Service
public class OperatorCartCleanupService {

	private final OperatorCartMapper operatorCartMapper;

	public OperatorCartCleanupService(OperatorCartMapper operatorCartMapper) {
		this.operatorCartMapper = operatorCartMapper;
	}

	public int deleteByCompanyIdAndItemIds(long companyId, Collection<Long> itemIds) {
		if (companyId < 1L || itemIds == null || itemIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<OperatorCart> w = new LambdaQueryWrapper<>();
		w.eq(OperatorCart::getCompanyId, companyId).in(OperatorCart::getItemId, itemIds);
		return operatorCartMapper.delete(w);
	}

	public int deleteByCartIds(Collection<Long> cartIds) {
		if (cartIds == null || cartIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<OperatorCart> w = new LambdaQueryWrapper<>();
		w.in(OperatorCart::getCartId, cartIds);
		return operatorCartMapper.delete(w);
	}

	public int deleteAllRows(List<OperatorCart> cartRows) {
		if (cartRows == null || cartRows.isEmpty()) {
			return 0;
		}
		List<Long> cartIds =
				cartRows.stream()
						.map(OperatorCart::getCartId)
						.filter(id -> id != null && id > 0L)
						.distinct()
						.toList();
		return deleteByCartIds(cartIds);
	}
}
