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

package cn.shopex.ecshopx.goods.service.members;

import cn.shopex.ecshopx.common.members.port.MembersSubscribeNoticeItemNamePort;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MembersSubscribeNoticeItemNamePortImpl implements MembersSubscribeNoticeItemNamePort {

	private final ItemsMapper itemsMapper;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;

	public MembersSubscribeNoticeItemNamePortImpl(
			ItemsMapper itemsMapper, ItemsListMultiLangApplier itemsListMultiLangApplier) {
		this.itemsMapper = itemsMapper;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
	}

	@Override
	public boolean itemExists(long companyId, long itemId) {
		if (itemId <= 0L) {
			return false;
		}
		Long cnt =
				itemsMapper.selectCount(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.eq(Items::getItemId, itemId));
		return cnt != null && cnt > 0L;
	}

	@Override
	public String resolveItemName(long companyId, long itemId, String acceptLanguageHeader) {
		if (itemId == 0L) {
			return "";
		}
		Items one =
				itemsMapper.selectOne(
						new LambdaQueryWrapper<Items>()
								.eq(Items::getCompanyId, companyId)
								.eq(Items::getItemId, itemId)
								.select(Items::getItemId, Items::getItemName)
								.last("LIMIT 1"));
		if (one == null) {
			return "";
		}
		String lang =
				StringUtils.hasText(acceptLanguageHeader) ? acceptLanguageHeader.trim() : "zh-CN";
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", one.getItemId());
		row.put("item_name", one.getItemName() == null ? "" : one.getItemName());
		List<Map<String, Object>> rows = new ArrayList<>(1);
		rows.add(row);
		itemsListMultiLangApplier.applyToRows(companyId, lang, rows);
		Object name = rows.get(0).get("item_name");
		if (name == null) {
			return "";
		}
		return String.valueOf(name);
	}
}
