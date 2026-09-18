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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.goods.domain.ItemsGroupRelItem;
import cn.shopex.ecshopx.goods.mapper.ItemsGroupRelItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ItemsGroupGetGroupItemsService {

	private final ItemsGroupRelItemMapper itemsGroupRelItemMapper;

	public ItemsGroupGetGroupItemsService(ItemsGroupRelItemMapper itemsGroupRelItemMapper) {
		this.itemsGroupRelItemMapper = itemsGroupRelItemMapper;
	}

	public List<Long> listGoodsIdsByGroupId(Long groupId, int page, int pageSize) {
		Page<ItemsGroupRelItem> mpPage = new Page<>(page, pageSize);
		LambdaQueryWrapper<ItemsGroupRelItem> w = Wrappers.lambdaQuery(ItemsGroupRelItem.class)
				.select(ItemsGroupRelItem::getGoodsId)
				.eq(ItemsGroupRelItem::getGroupId, groupId)
				.orderByDesc(ItemsGroupRelItem::getId);
		itemsGroupRelItemMapper.selectPage(mpPage, w);
		return mpPage.getRecords().stream().map(ItemsGroupRelItem::getGoodsId).toList();
	}

	public List<Long> listAllGoodsIdsByGroupId(long groupId) {
		List<ItemsGroupRelItem> rows = itemsGroupRelItemMapper.selectList(Wrappers.<ItemsGroupRelItem>lambdaQuery()
				.eq(ItemsGroupRelItem::getGroupId, groupId)
				.select(ItemsGroupRelItem::getGoodsId, ItemsGroupRelItem::getId)
				.orderByDesc(ItemsGroupRelItem::getId));
		Set<Long> ordered = new LinkedHashSet<>();
		for (ItemsGroupRelItem r : rows) {
			Long gid = r.getGoodsId();
			if (gid != null && gid > 0L) {
				ordered.add(gid);
			}
		}
		return new ArrayList<>(ordered);
	}
}
