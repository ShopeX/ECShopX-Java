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

package cn.shopex.ecshopx.superadmin.service;

import cn.shopex.ecshopx.superadmin.domain.ShopNotice;
import cn.shopex.ecshopx.superadmin.mapper.ShopNoticeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ShopNoticeListService {

	private final ShopNoticeMapper shopNoticeMapper;

	public ShopNoticeListService(ShopNoticeMapper shopNoticeMapper) {
		this.shopNoticeMapper = shopNoticeMapper;
	}

	public Map<String, Object> list(int page, int pageSize) {
		LambdaQueryWrapper<ShopNotice> w = new LambdaQueryWrapper<>();
		w.orderByDesc(ShopNotice::getNoticeId);

		Page<ShopNotice> mpPage = new Page<>(page, pageSize);
		Page<ShopNotice> result = shopNoticeMapper.selectPage(mpPage, w);

		int totalCount = (int) result.getTotal();
		List<Map<String, Object>> list = new ArrayList<>();
		for (ShopNotice e : result.getRecords()) {
			list.add(toNoticeRow(e));
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", list);
		return out;
	}

	public Optional<Map<String, Object>> findNoticeRowByNoticeId(long noticeId) {
		ShopNotice entity = shopNoticeMapper.selectById(noticeId);
		if (entity == null) {
			return Optional.empty();
		}
		return Optional.of(toNoticeRow(entity));
	}

	private static Map<String, Object> toNoticeRow(ShopNotice e) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("notice_id", e.getNoticeId());
		row.put("type", e.getType());
		row.put("title", e.getTitle());
		row.put("web_link", e.getWebLink());
		row.put("is_publish", e.getIsPublish());
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());
		return row;
	}
}
