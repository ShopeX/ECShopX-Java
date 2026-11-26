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

package cn.shopex.ecshopx.goods.service.hotword;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.domain.SearchHotword;
import cn.shopex.ecshopx.goods.mapper.SearchHotwordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 热词管理端服务（ECX-11390 F-001/F-002/F-003）：列表、详情、保存、删除。
 *
 * <p>全部操作按 {@code company_id} 隔离；状态与有效期实时计算，见 {@link SearchHotwordStatus}。
 */
@Service
public class SearchHotwordAdminService {

	private final SearchHotwordMapper searchHotwordMapper;

	public SearchHotwordAdminService(SearchHotwordMapper searchHotwordMapper) {
		this.searchHotwordMapper = searchHotwordMapper;
	}

	/** 列表筛选条件。 */
	public record ListQuery(
			String content,
			String channel,
			Integer status,
			Integer startTimeBegin,
			Integer startTimeEnd,
			Integer endTimeBegin,
			Integer endTimeEnd,
			long page,
			long pageSize) {}

	/** 热词列表：支持热词模糊、渠道、状态与展示开始/结束时间区间筛选。 */
	public Map<String, Object> list(long companyId, ListQuery query, long nowEpochSecond) {
		long page = query.page() < 1L ? 1L : query.page();
		long pageSize = query.pageSize() < 1L ? 10L : Math.min(query.pageSize(), 100L);

		LambdaQueryWrapper<SearchHotword> wrapper =
				new LambdaQueryWrapper<SearchHotword>().eq(SearchHotword::getCompanyId, companyId);
		if (StringUtils.hasText(query.content())) {
			wrapper.like(SearchHotword::getContent, query.content().trim());
		}
		if (StringUtils.hasText(query.channel())) {
			wrapper.eq(SearchHotword::getChannel, query.channel().trim());
		}
		SearchHotwordStatus.applyListFilter(wrapper, query.status(), nowEpochSecond);
		if (query.startTimeBegin() != null) {
			wrapper.ge(SearchHotword::getStartTime, query.startTimeBegin());
		}
		if (query.startTimeEnd() != null) {
			wrapper.le(SearchHotword::getStartTime, query.startTimeEnd());
		}
		if (query.endTimeBegin() != null) {
			wrapper.ge(SearchHotword::getEndTime, query.endTimeBegin());
		}
		if (query.endTimeEnd() != null) {
			wrapper.le(SearchHotword::getEndTime, query.endTimeEnd());
		}
		wrapper
				.orderByDesc(SearchHotword::getSort)
				.orderByDesc(SearchHotword::getCreated)
				.orderByDesc(SearchHotword::getId);

		Page<SearchHotword> mpPage = new Page<>(page, pageSize);
		searchHotwordMapper.selectPage(mpPage, wrapper);

		List<Map<String, Object>> list = new ArrayList<>();
		for (SearchHotword row : mpPage.getRecords()) {
			list.add(SearchHotwordRows.toAdminRow(row, nowEpochSecond));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", mpPage.getTotal());
		out.put("list", list);
		return out;
	}

	/** 热词详情；不存在或不属于当前公司返回 {@code null}。 */
	public Map<String, Object> detail(long companyId, long id, long nowEpochSecond) {
		SearchHotword row = findOwnedRow(companyId, id);
		return row == null ? null : SearchHotwordRows.toAdminRow(row, nowEpochSecond);
	}

	/**
	 * 新增或编辑热词。{@code id} 为空表示新增；编辑时记录必须仍属于当前公司，否则拒绝覆盖并提示刷新。
	 */
	public Map<String, Object> save(
			long companyId, Long id, SearchHotwordInputNormalizer.Normalized input, long nowEpochSecond) {
		SearchHotword saved;
		if (id == null) {
			saved = new SearchHotword();
			saved.setCompanyId(companyId);
			applyFields(saved, input);
			saved.setCreated(Integer.valueOf((int) nowEpochSecond));
			saved.setUpdated(saved.getCreated());
			searchHotwordMapper.insert(saved);
		} else {
			LambdaUpdateWrapper<SearchHotword> wrapper = new LambdaUpdateWrapper<SearchHotword>()
					.eq(SearchHotword::getId, id)
					.eq(SearchHotword::getCompanyId, companyId)
					.set(SearchHotword::getContent, input.content())
					.set(SearchHotword::getType, input.type())
					.set(SearchHotword::getTextColor, input.textColor())
					.set(SearchHotword::getBackgroundColor, input.backgroundColor())
					.set(SearchHotword::getIconUrl, input.iconUrl())
					.set(SearchHotword::getImageUrl, input.imageUrl())
					.set(SearchHotword::getSort, input.sort())
					.set(SearchHotword::getStartTime, input.startTime())
					.set(SearchHotword::getEndTime, input.endTime())
					.set(SearchHotword::getChannel, input.channel())
					.set(SearchHotword::getJumpPage, input.jumpPage())
					.set(SearchHotword::getUpdated, Integer.valueOf((int) nowEpochSecond));
			int affected = searchHotwordMapper.update(null, wrapper);
			if (affected <= 0) {
				throw notFoundForEdit();
			}
			saved = new SearchHotword();
			saved.setId(id);
			saved.setCompanyId(companyId);
			applyFields(saved, input);
			saved.setUpdated(Integer.valueOf((int) nowEpochSecond));
			SearchHotword fresh = searchHotwordMapper.selectById(id);
			if (fresh != null) {
				saved = fresh;
			}
		}
		return SearchHotwordRows.toAdminRow(saved, nowEpochSecond);
	}

	/** 删除热词；命中返回 {@code true}，不存在或非本公司返回 {@code false}。 */
	public boolean delete(long companyId, long id) {
		int affected = searchHotwordMapper.delete(new LambdaQueryWrapper<SearchHotword>()
				.eq(SearchHotword::getId, id)
				.eq(SearchHotword::getCompanyId, companyId));
		return affected > 0;
	}

	private SearchHotword findOwnedRow(long companyId, long id) {
		SearchHotword row = searchHotwordMapper.selectById(id);
		if (row == null || row.getCompanyId() == null || !row.getCompanyId().equals(companyId)) {
			return null;
		}
		return row;
	}

	private static void applyFields(SearchHotword row, SearchHotwordInputNormalizer.Normalized input) {
		row.setContent(input.content());
		row.setType(input.type());
		row.setTextColor(input.textColor());
		row.setBackgroundColor(input.backgroundColor());
		row.setIconUrl(input.iconUrl());
		row.setImageUrl(input.imageUrl());
		row.setSort(input.sort());
		row.setStartTime(input.startTime());
		row.setEndTime(input.endTime());
		row.setChannel(input.channel());
		row.setJumpPage(input.jumpPage());
	}

	private static BadRequestException notFoundForEdit() {
		return new BadRequestException(
				"热词不存在或已删除，请刷新后重试", Map.of("id", List.of("validation.exists")), 422);
	}
}
