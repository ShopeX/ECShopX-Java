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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.repository.ItemsQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import cn.shopex.ecshopx.promotions.dto.ItemActivityCheckRow;
import cn.shopex.ecshopx.promotions.service.ItemsTagActivityCheckService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NormalGoodsTagImportRowService {

	private final ItemsRepository itemsRepository;
	private final ItemsTagsRepository itemsTagsRepository;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ItemsQueryRepository itemsQueryRepository;
	private final ItemsTagActivityCheckService itemsTagActivityCheckService;

	public NormalGoodsTagImportRowService(
			ItemsRepository itemsRepository,
			ItemsTagsRepository itemsTagsRepository,
			ItemsRelTagsRepository itemsRelTagsRepository,
			ItemsQueryRepository itemsQueryRepository,
			ItemsTagActivityCheckService itemsTagActivityCheckService) {
		this.itemsRepository = itemsRepository;
		this.itemsTagsRepository = itemsTagsRepository;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.itemsQueryRepository = itemsQueryRepository;
		this.itemsTagActivityCheckService = itemsTagActivityCheckService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void applyRow(long companyId, long distributorIdForTagScope, Map<String, Object> row) {
		Map<String, String> r = trimRow(row);
		String itemBn = r.get("item_bn");
		String tagName = r.get("tag_name");
		if (!StringUtils.hasText(itemBn)) {
			throw new BadRequestException("缺少必填字段: item_bn");
		}
		if (!StringUtils.hasText(tagName)) {
			throw new BadRequestException("缺少必填字段: tag_name");
		}
		Items item = itemsRepository.findByItemBnAndCompany(itemBn, companyId);
		if (item == null || item.getItemId() == null || item.getItemId() <= 0L) {
			throw new ResourceException("未查询到对应商品");
		}
		long itemId = item.getItemId();
		long distScope = distributorIdForTagScope > 0L ? distributorIdForTagScope : 0L;
		ItemsTags tag = itemsTagsRepository.selectByCompanyAndTagNameAndDistributor(companyId, tagName, distScope);
		if (tag == null && distScope > 0L) {
			tag = itemsTagsRepository.selectByCompanyAndTagNameAndDistributor(companyId, tagName, 0L);
		}
		if (tag == null || tag.getTagId() == null || tag.getTagId() <= 0L) {
			throw new ResourceException("未查询到对应标签");
		}
		long tagId = tag.getTagId();
		List<ItemsRelTags> existing = itemsRelTagsRepository.getLists(companyId, List.of(itemId));
		for (ItemsRelTags rel : existing) {
			if (rel.getTagId() != null && rel.getTagId().longValue() == tagId) {
				return;
			}
		}
		List<Items> items = itemsQueryRepository.listByCompanyIdAndItemIds(companyId, List.of(itemId));
		if (!items.isEmpty()) {
			AtomicReference<String> errRef = new AtomicReference<>("商品标签导致活动冲突");
			List<ItemActivityCheckRow> rows = toCheckRows(items, List.of(itemId));
			boolean ok = itemsTagActivityCheckService.checkActivity(rows, List.of(tagId), companyId, errRef);
			if (!ok) {
				String msg = errRef.get();
				if (msg == null || msg.isBlank()) {
					msg = "商品标签导致活动冲突";
				}
				throw new ResourceException(msg);
			}
		}
		ItemsRelTags relRow = new ItemsRelTags();
		relRow.setCompanyId(companyId);
		relRow.setItemId(itemId);
		relRow.setTagId(tagId);
		itemsRelTagsRepository.create(relRow);
	}

	private static List<ItemActivityCheckRow> toCheckRows(List<Items> items, List<Long> requestItemIds) {
		List<Long> reqCopy = new ArrayList<>(requestItemIds);
		List<ItemActivityCheckRow> rows = new ArrayList<>();
		for (Items it : items) {
			ItemActivityCheckRow r = new ItemActivityCheckRow();
			r.setItemId(it.getItemId());
			r.setMainCatId(parseLongOrNull(it.getItemCategory()));
			r.setBrandId(it.getBrandId() != null ? it.getBrandId().longValue() : null);
			r.setAllRequestItemIds(reqCopy);
			rows.add(r);
		}
		return rows;
	}

	private static Long parseLongOrNull(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Map<String, String> trimRow(Map<String, Object> row) {
		Map<String, String> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : row.entrySet()) {
			Object v = e.getValue();
			out.put(e.getKey(), v == null ? "" : String.valueOf(v).trim());
		}
		return out;
	}
}
