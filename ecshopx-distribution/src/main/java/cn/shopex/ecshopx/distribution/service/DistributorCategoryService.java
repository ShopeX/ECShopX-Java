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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.DistributorCategory;
import cn.shopex.ecshopx.distribution.mapper.DistributorCategoryMapper;
import cn.shopex.ecshopx.distribution.support.DistributorCategoryRowMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DistributorCategoryService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final DistributorCategoryMapper distributorCategoryMapper;

	public DistributorCategoryService(DistributorCategoryMapper distributorCategoryMapper) {
		this.distributorCategoryMapper = distributorCategoryMapper;
	}

	public Map<String, Object> list(long companyId, int page, int pageSize, String categoryNameLike) {
		LambdaQueryWrapper<DistributorCategory> w = new LambdaQueryWrapper<>();
		w.eq(DistributorCategory::getCompanyId, companyId);
		if (StringUtils.hasText(categoryNameLike)) {
			String escaped = escapeSqlLike(categoryNameLike.trim());
			w.like(DistributorCategory::getCategoryName, "%" + escaped + "%");
		}
		w.orderByDesc(DistributorCategory::getCreated);

		Page<DistributorCategory> p = new Page<>(page, pageSize);
		distributorCategoryMapper.selectPage(p, w);

		List<Map<String, Object>> list = new ArrayList<>();
		for (DistributorCategory row : p.getRecords()) {
			list.add(DistributorCategoryRowMapper.toRow(row));
		}
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", (int) p.getTotal());
		data.put("list", list);
		return data;
	}

	public Map<String, Object> getInfo(long companyId, long categoryId) {
		DistributorCategory entity = selectOne(companyId, categoryId);
		if (entity == null) {
			return Map.of();
		}
		return DistributorCategoryRowMapper.toRow(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> create(long companyId, String categoryName) {
		long now = System.currentTimeMillis() / 1000L;
		DistributorCategory entity = new DistributorCategory();
		entity.setCompanyId(companyId);
		entity.setCategoryName(categoryName.trim());
		entity.setCategoryCode(generateCategoryCode());
		entity.setCreated(now);
		entity.setUpdated(now);
		try {
			distributorCategoryMapper.insert(entity);
		} catch (DataIntegrityViolationException ex) {
			throw new ResourceException("数据保存失败");
		}
		return DistributorCategoryRowMapper.toRow(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> update(long companyId, long categoryId, String categoryName, String categoryCode) {
		DistributorCategory entity = selectOne(companyId, categoryId);
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}
		entity.setCategoryName(categoryName.trim());
		entity.setCategoryCode(categoryCode.trim());
		entity.setUpdated(System.currentTimeMillis() / 1000L);
		try {
			distributorCategoryMapper.updateById(entity);
		} catch (DataIntegrityViolationException ex) {
			throw new ResourceException("数据保存失败");
		}
		return DistributorCategoryRowMapper.toRow(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	public void delete(long companyId, long categoryId) {
		DistributorCategory entity = selectOne(companyId, categoryId);
		if (entity == null) {
			throw new ResourceException("分类不存在");
		}
		try {
			LambdaQueryWrapper<DistributorCategory> w = new LambdaQueryWrapper<DistributorCategory>()
					.eq(DistributorCategory::getCategoryId, categoryId)
					.eq(DistributorCategory::getCompanyId, companyId);
			distributorCategoryMapper.delete(w);
		} catch (DataIntegrityViolationException ex) {
			throw new ResourceException("数据保存失败");
		}
	}

	/**
	 * 批量回填 {@code distributor_category_name}。对齐 PHP
	 * {@code DistributorService::appendDistributorCategoryName}：{@code array_filter} 去掉 0；找不到名称则为空串。
	 */
	public void appendDistributorCategoryName(long companyId, List<Map<String, Object>> distributorList) {
		if (distributorList == null || distributorList.isEmpty()) {
			return;
		}
		Set<Long> categoryIds = new LinkedHashSet<>();
		for (Map<String, Object> row : distributorList) {
			Long id = toPositiveLong(row.get("distributor_category_id"));
			if (id != null) {
				categoryIds.add(id);
			}
		}
		Map<Long, String> categoryMap = mapCategoryNames(companyId, categoryIds);
		for (Map<String, Object> row : distributorList) {
			Long id = toLongOrZero(row.get("distributor_category_id"));
			row.put("distributor_category_name", categoryMap.getOrDefault(id, ""));
		}
	}

	/** 按公司 + 分类名称精确匹配；未命中返回 null（导入场景）。 */
	public Long getCategoryIdByName(long companyId, String categoryName) {
		if (!StringUtils.hasText(categoryName)) {
			return null;
		}
		LambdaQueryWrapper<DistributorCategory> w = new LambdaQueryWrapper<DistributorCategory>()
				.eq(DistributorCategory::getCompanyId, companyId)
				.eq(DistributorCategory::getCategoryName, categoryName.trim())
				.last("LIMIT 1");
		DistributorCategory entity = distributorCategoryMapper.selectOne(w);
		return entity == null ? null : entity.getCategoryId();
	}

	private Map<Long, String> mapCategoryNames(long companyId, Collection<Long> categoryIds) {
		Map<Long, String> out = new HashMap<>();
		if (categoryIds == null || categoryIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<DistributorCategory> w = new LambdaQueryWrapper<DistributorCategory>()
				.eq(DistributorCategory::getCompanyId, companyId)
				.in(DistributorCategory::getCategoryId, categoryIds)
				.select(DistributorCategory::getCategoryId, DistributorCategory::getCategoryName);
		List<DistributorCategory> rows = distributorCategoryMapper.selectList(w);
		for (DistributorCategory row : rows) {
			if (row.getCategoryId() != null) {
				out.put(row.getCategoryId(), row.getCategoryName() == null ? "" : row.getCategoryName());
			}
		}
		return out;
	}

	private DistributorCategory selectOne(long companyId, long categoryId) {
		LambdaQueryWrapper<DistributorCategory> w = new LambdaQueryWrapper<DistributorCategory>()
				.eq(DistributorCategory::getCategoryId, categoryId)
				.eq(DistributorCategory::getCompanyId, companyId);
		return distributorCategoryMapper.selectOne(w);
	}

	private static String generateCategoryCode() {
		byte[] bytes = new byte[4];
		RANDOM.nextBytes(bytes);
		StringBuilder sb = new StringBuilder(8);
		for (byte b : bytes) {
			sb.append(String.format("%02X", b & 0xFF));
		}
		return sb.toString();
	}

	private static String escapeSqlLike(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static Long toPositiveLong(Object o) {
		Long v = toLongOrNull(o);
		return (v != null && v > 0L) ? v : null;
	}

	private static long toLongOrZero(Object o) {
		Long v = toLongOrNull(o);
		return v == null ? 0L : v;
	}

	private static Long toLongOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
