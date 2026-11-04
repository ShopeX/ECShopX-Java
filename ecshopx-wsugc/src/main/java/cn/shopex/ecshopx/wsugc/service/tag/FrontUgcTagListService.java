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

package cn.shopex.ecshopx.wsugc.service.tag;

import cn.shopex.ecshopx.wsugc.domain.Tag;
import cn.shopex.ecshopx.wsugc.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontUgcTagListService {

	private final TagMapper tagMapper;
	private final TagDetailService tagDetailService;

	public FrontUgcTagListService(TagMapper tagMapper, TagDetailService tagDetailService) {
		this.tagMapper = tagMapper;
		this.tagDetailService = tagDetailService;
	}

	public Object buildH5TagList(long companyId, int page, int pageSize, String tagName, String sort) {
		LambdaQueryWrapper<Tag> w = new LambdaQueryWrapper<>();
		w.eq(Tag::getCompanyId, companyId).eq(Tag::getEnabled, 1).eq(Tag::getStatus, 1);

		boolean nameFilter = tagName != null && !tagName.isEmpty();
		if (nameFilter) {
			w.like(Tag::getTagName, escapeLike(tagName) + "%");
		}

		applySort(w, sort);

		Long totalCount = tagMapper.selectCount(w);
		if (totalCount == null || totalCount == 0L) {
			return Collections.emptyList();
		}

		List<Tag> tags;
		if (pageSize > 0) {
			Page<Tag> p = new Page<>(page, pageSize, false);
			tagMapper.selectPage(p, w);
			tags = p.getRecords() != null ? p.getRecords() : Collections.emptyList();
		} else {
			tags = tagMapper.selectList(w);
			if (tags == null) {
				tags = Collections.emptyList();
			}
		}

		List<LinkedHashMap<String, Object>> rows = new ArrayList<>();
		for (Tag entity : tags) {
			LinkedHashMap<String, Object> row = tagDetailService.toH5ListRowMap(entity);
			if (nameFilter) {
				long tid = entity.getTagId() != null ? entity.getTagId() : 0L;
				long rank = String.valueOf(row.get("tag_name")).equals(tagName) ? -1L : tid;
				row.put("rank", rank);
				row = ksortRowMap(row);
			}
			rows.add(row);
		}

		if (nameFilter) {
			rows.sort(
					Comparator.comparingLong((LinkedHashMap<String, Object> m) -> longFromMap(m, "rank"))
							.thenComparingLong(m -> longFromMap(m, "tag_id")));
		}

		Map<String, Object> body = new TreeMap<>();
		body.put("list", rows);
		body.put("total_count", totalCount);
		return body;
	}

	private static String escapeLike(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static void applySort(LambdaQueryWrapper<Tag> wrapper, String sort) {
		if (sort == null || sort.trim().isEmpty()) {
			wrapper.orderByAsc(Tag::getPOrder).orderByDesc(Tag::getCreated);
			return;
		}
		String trimmed = sort.trim();
		int sp = trimmed.indexOf(' ');
		if (sp <= 0 || sp >= trimmed.length() - 1) {
			wrapper.orderByAsc(Tag::getPOrder).orderByDesc(Tag::getCreated);
			return;
		}
		String field = trimmed.substring(0, sp).trim();
		String direction = trimmed.substring(sp + 1).trim();
		if (!StringUtils.hasText(field) || !StringUtils.hasText(direction)) {
			wrapper.orderByAsc(Tag::getPOrder).orderByDesc(Tag::getCreated);
			return;
		}
		String dirLower = direction.toLowerCase();
		boolean asc;
		if ("asc".equals(dirLower)) {
			asc = true;
		} else if ("desc".equals(dirLower)) {
			asc = false;
		} else {
			wrapper.orderByAsc(Tag::getPOrder).orderByDesc(Tag::getCreated);
			return;
		}
		switch (field) {
			case "p_order" -> {
				if (asc) {
					wrapper.orderByAsc(Tag::getPOrder);
				} else {
					wrapper.orderByDesc(Tag::getPOrder);
				}
			}
			case "created" -> {
				if (asc) {
					wrapper.orderByAsc(Tag::getCreated);
				} else {
					wrapper.orderByDesc(Tag::getCreated);
				}
			}
			case "updated" -> {
				if (asc) {
					wrapper.orderByAsc(Tag::getUpdated);
				} else {
					wrapper.orderByDesc(Tag::getUpdated);
				}
			}
			case "tag_id" -> {
				if (asc) {
					wrapper.orderByAsc(Tag::getTagId);
				} else {
					wrapper.orderByDesc(Tag::getTagId);
				}
			}
			case "user_id" -> {
				if (asc) {
					wrapper.orderByAsc(Tag::getUserId);
				} else {
					wrapper.orderByDesc(Tag::getUserId);
				}
			}
			case "status" -> {
				if (asc) {
					wrapper.orderByAsc(Tag::getStatus);
				} else {
					wrapper.orderByDesc(Tag::getStatus);
				}
			}
			case "enabled" -> {
				if (asc) {
					wrapper.orderByAsc(Tag::getEnabled);
				} else {
					wrapper.orderByDesc(Tag::getEnabled);
				}
			}
			case "source" -> {
				if (asc) {
					wrapper.orderByAsc(Tag::getSource);
				} else {
					wrapper.orderByDesc(Tag::getSource);
				}
			}
			case "operator_id" -> {
				if (asc) {
					wrapper.orderByAsc(Tag::getOperatorId);
				} else {
					wrapper.orderByDesc(Tag::getOperatorId);
				}
			}
			case "company_id" -> {
				if (asc) {
					wrapper.orderByAsc(Tag::getCompanyId);
				} else {
					wrapper.orderByDesc(Tag::getCompanyId);
				}
			}
			case "tag_name" -> {
				if (asc) {
					wrapper.orderByAsc(Tag::getTagName);
				} else {
					wrapper.orderByDesc(Tag::getTagName);
				}
			}
			default -> {
				wrapper.orderByAsc(Tag::getPOrder).orderByDesc(Tag::getCreated);
				return;
			}
		}
		if (!"p_order".equals(field)) {
			wrapper.orderByAsc(Tag::getPOrder);
		}
	}

	private static LinkedHashMap<String, Object> ksortRowMap(Map<String, Object> src) {
		TreeMap<String, Object> sorted = new TreeMap<>(src);
		return new LinkedHashMap<>(sorted);
	}

	private static long longFromMap(Map<String, Object> m, String key) {
		Object v = m.get(key);
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
