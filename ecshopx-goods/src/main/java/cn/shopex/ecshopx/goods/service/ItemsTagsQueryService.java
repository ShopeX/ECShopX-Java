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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsListFilter;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsTagsQueryService {

	private final ItemsTagsRepository itemsTagsRepository;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final ItemsTagsMultiLangApplier itemsTagsMultiLangApplier;
	private final DistributorMapper distributorMapper;

	public ItemsTagsQueryService(ItemsTagsRepository itemsTagsRepository, ItemsRelTagsRepository itemsRelTagsRepository,
			ItemsTagsMultiLangApplier itemsTagsMultiLangApplier, DistributorMapper distributorMapper) {
		this.itemsTagsRepository = itemsTagsRepository;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.itemsTagsMultiLangApplier = itemsTagsMultiLangApplier;
		this.distributorMapper = distributorMapper;
	}

	public List<String> getItemIdsByTagIds(long companyId, List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return List.of();
		}
		List<Long> itemIds = itemsRelTagsRepository.listItemIdsByCompanyIdAndTagIds(companyId, tagIds);
		return itemIds.stream().map(String::valueOf).toList();
	}

	public Object getTagInfoByTagId(long tagId) {
		ItemsTags entity = itemsTagsRepository.selectById(tagId);
		if (entity == null) {
			return Collections.emptyList();
		}
		return ItemsTagsRowMaps.toTagRowMap(entity);
	}

	public Map<String, Object> getTagsList(long companyId, long jwtDistributorId, int page, int pageSize, String tagName,
			boolean frontShowSpecified, Integer frontShowValue, String countryCode, String tagSource, Boolean isPlatform,
			String distributorIdQueryStr) {
		validatePaging(page, pageSize);

		String countryCodeResolved = StringUtils.hasText(countryCode) ? countryCode.trim() : "zh-CN";

		ItemsTagsListFilter filter = new ItemsTagsListFilter();
		filter.setCompanyId(companyId);
		if (frontShowSpecified && frontShowValue != null) {
			filter.setFrontShowSpecified(true);
			filter.setFrontShowValue(frontShowValue);
		} else {
			filter.setFrontShowSpecified(false);
		}

		if (StringUtils.hasText(tagName) && StringUtils.hasText(tagName.trim())) {
			String keyword = tagName.trim();
			List<Long> merged = itemsTagsRepository.selectTagIdsByKeywordForCompany(companyId, keyword, countryCodeResolved);
			if (!merged.isEmpty()) {
				filter.setTagIdsIn(merged);
			} else {
				filter.setTagNameContains(keyword);
			}
		}

		boolean tagSourceHandled = false;
		if (StringUtils.hasText(tagSource)) {
			String ts = tagSource.trim();
			if ("distributor".equalsIgnoreCase(ts)) {
				filter.setDistributorMode(ItemsTagsListFilter.DistributorMode.GT_ZERO);
				tagSourceHandled = true;
			} else if ("platform".equalsIgnoreCase(ts)) {
				filter.setDistributorMode(ItemsTagsListFilter.DistributorMode.EQ);
				filter.setDistributorEq(0L);
				tagSourceHandled = true;
			}
		}
		if (!tagSourceHandled) {
			long q = resolveDistributorQ(jwtDistributorId, distributorIdQueryStr);
			boolean platformFlag = Boolean.TRUE.equals(isPlatform);
			if (platformFlag) {
				filter.setDistributorMode(ItemsTagsListFilter.DistributorMode.IN);
				filter.setDistributorIn(List.of(0L, q));
			} else {
				filter.setDistributorMode(ItemsTagsListFilter.DistributorMode.EQ);
				filter.setDistributorEq(q);
			}
		}

		if (StringUtils.hasText(distributorIdQueryStr) && jwtDistributorId == 0L
				&& filter.getDistributorMode() != ItemsTagsListFilter.DistributorMode.IN) {
			long parsed = parseRequiredDistributorId(distributorIdQueryStr);
			filter.setDistributorMode(ItemsTagsListFilter.DistributorMode.EQ);
			filter.setDistributorEq(parsed);
		}

		if (pageSize == 500) {
			if (filter.getDistributorMode() == ItemsTagsListFilter.DistributorMode.EQ && filter.getDistributorEq() != null
					&& filter.getDistributorEq() != 0L) {
				filter.setDistributorMode(ItemsTagsListFilter.DistributorMode.IN);
				filter.setDistributorIn(List.of(0L, filter.getDistributorEq()));
				filter.setDistributorEq(null);
			} else if (filter.getDistributorMode() == ItemsTagsListFilter.DistributorMode.IN && filter.getDistributorIn() != null
					&& !filter.getDistributorIn().isEmpty()) {
				LinkedHashSet<Long> set = new LinkedHashSet<>(filter.getDistributorIn());
				set.add(0L);
				filter.setDistributorIn(new ArrayList<>(set));
			}
		}

		Page<ItemsTags> pageReq = new Page<>(page, pageSize);
		IPage<ItemsTags> pageResult = itemsTagsRepository.selectPageByFilter(pageReq, filter);
		long total = pageResult.getTotal();
		List<ItemsTags> records = pageResult.getRecords();

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		List<Map<String, Object>> rowMaps = new ArrayList<>();
		for (ItemsTags rec : records) {
			rowMaps.add(ItemsTagsRowMaps.toTagRowMap(rec));
		}
		itemsTagsMultiLangApplier.applyListLangForTags(companyId, countryCodeResolved, rowMaps);

		if (total > 0) {
			Set<Long> distIds = new LinkedHashSet<>();
			for (Map<String, Object> row : rowMaps) {
				Object d = row.get("distributor_id");
				long did = 0L;
				if (d instanceof Number n) {
					did = n.longValue();
				} else if (d != null) {
					try {
						did = Long.parseLong(d.toString());
					} catch (NumberFormatException ignored) {
						did = 0L;
					}
				}
				if (did > 0L) {
					distIds.add(did);
				}
			}
			Map<Long, String> idToName = new LinkedHashMap<>();
			if (!distIds.isEmpty()) {
				LambdaQueryWrapper<Distributor> dw = new LambdaQueryWrapper<>();
				dw.eq(Distributor::getCompanyId, companyId).in(Distributor::getDistributorId, distIds).select(Distributor::getDistributorId,
						Distributor::getName);
				List<Distributor> dists = distributorMapper.selectList(dw);
				for (Distributor dist : dists) {
					if (dist.getDistributorId() != null && StringUtils.hasText(dist.getName())) {
						idToName.put(dist.getDistributorId(), dist.getName());
					}
				}
			}
			for (Map<String, Object> row : rowMaps) {
				Object d = row.get("distributor_id");
				long did = 0L;
				if (d instanceof Number n) {
					did = n.longValue();
				} else if (d != null) {
					try {
						did = Long.parseLong(d.toString());
					} catch (NumberFormatException ignored) {
						did = 0L;
					}
				}
				row.put("distributor_name", "平台");
				row.put("is_platform", true);
				if (did > 0L) {
					String name = idToName.get(did);
					if (StringUtils.hasText(name)) {
						row.put("distributor_name", name);
						row.put("is_platform", false);
					}
				}
			}
		}

		out.put("list", rowMaps);
		return out;
	}

	private static long resolveDistributorQ(long jwtDistributorId, String distributorIdQueryStr) {
		if (StringUtils.hasText(distributorIdQueryStr)) {
			return parseRequiredDistributorId(distributorIdQueryStr);
		}
		return jwtDistributorId;
	}

	private static long parseRequiredDistributorId(String distributorIdQueryStr) {
		try {
			return Long.parseLong(distributorIdQueryStr.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("distributor_id 无效");
		}
	}

	private static void validatePaging(int page, int pageSize) {
		if (page < 1) {
			throw new BadRequestException("分页参数错误");
		}
		if (pageSize < 1 || pageSize > 1000) {
			throw new BadRequestException("每页最多查询1000条数据");
		}
	}
}
