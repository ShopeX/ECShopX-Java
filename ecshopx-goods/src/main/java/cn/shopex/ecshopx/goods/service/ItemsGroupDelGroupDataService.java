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

import cn.shopex.ecshopx.goods.domain.ItemsGroup;
import cn.shopex.ecshopx.goods.domain.ItemsGroupRelItem;
import cn.shopex.ecshopx.goods.mapper.ItemsGroupMapper;
import cn.shopex.ecshopx.goods.mapper.ItemsGroupRelItemMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemsGroupDelGroupDataService {

	private final ItemsGroupMapper itemsGroupMapper;

	private final ItemsGroupRelItemMapper itemsGroupRelItemMapper;

	public ItemsGroupDelGroupDataService(
			ItemsGroupMapper itemsGroupMapper, ItemsGroupRelItemMapper itemsGroupRelItemMapper) {
		this.itemsGroupMapper = itemsGroupMapper;
		this.itemsGroupRelItemMapper = itemsGroupRelItemMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void delGroupData(String groupKey) {
		if (groupKey == null || groupKey.isBlank()) {
			return;
		}
		List<ItemsGroup> groups =
				itemsGroupMapper.selectList(Wrappers.lambdaQuery(ItemsGroup.class).eq(ItemsGroup::getGroupKey, groupKey));
		if (groups == null || groups.isEmpty()) {
			return;
		}
		for (ItemsGroup g : groups) {
			if (g == null || g.getId() == null) {
				continue;
			}
			Long gid = g.getId();
			itemsGroupRelItemMapper.delete(
					Wrappers.<ItemsGroupRelItem>lambdaQuery().eq(ItemsGroupRelItem::getGroupId, gid));
			itemsGroupMapper.deleteById(gid);
		}
	}
}
