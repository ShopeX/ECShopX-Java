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

package cn.shopex.ecshopx.goods.integration.kaquan;

import cn.shopex.ecshopx.common.openapi.OpenapiDiscountCardRelItemNamesPort;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OpenapiDiscountCardRelItemNamesPortImpl implements OpenapiDiscountCardRelItemNamesPort {

	private final ItemsMapper itemsMapper;

	public OpenapiDiscountCardRelItemNamesPortImpl(ItemsMapper itemsMapper) {
		this.itemsMapper = itemsMapper;
	}

	@Override
	public List<String> listItemNamesByItemIds(List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> wrapper = new LambdaQueryWrapper<>();
		wrapper.in(Items::getItemId, itemIds).select(Items::getItemId, Items::getItemName);
		return itemsMapper.selectList(wrapper).stream()
				.map(Items::getItemName)
				.filter(Objects::nonNull)
				.toList();
	}
}
