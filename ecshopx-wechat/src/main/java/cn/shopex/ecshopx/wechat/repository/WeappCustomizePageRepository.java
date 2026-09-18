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

package cn.shopex.ecshopx.wechat.repository;

import cn.shopex.ecshopx.wechat.domain.WeappCustomizePage;
import cn.shopex.ecshopx.wechat.mapper.WeappCustomizePageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class WeappCustomizePageRepository {

	private final WeappCustomizePageMapper mapper;

	public WeappCustomizePageRepository(WeappCustomizePageMapper mapper) {
		this.mapper = mapper;
	}

	public List<WeappCustomizePage> listByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<WeappCustomizePage> w = new LambdaQueryWrapper<>();
		w.in(WeappCustomizePage::getId, ids);
		return mapper.selectList(w);
	}

	/**
	 * 返回在库中且已开启、类型为分类页的页面 id。
	 */
	public Set<Long> listValidCategoryPageIds(long companyId, Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Set.of();
		}
		LambdaQueryWrapper<WeappCustomizePage> w = new LambdaQueryWrapper<>();
		w.eq(WeappCustomizePage::getCompanyId, companyId).eq(WeappCustomizePage::getIsOpen, 1).eq(WeappCustomizePage::getPageType, "category")
				.in(WeappCustomizePage::getId, ids);
		List<WeappCustomizePage> rows = mapper.selectList(w);
		Set<Long> out = new HashSet<>();
		for (WeappCustomizePage p : rows) {
			if (p.getId() != null) {
				out.add(p.getId());
			}
		}
		return out;
	}

	public long countOpenMyForCompany(long companyId, String templateNameOrNull) {
		LambdaQueryWrapper<WeappCustomizePage> w = new LambdaQueryWrapper<>();
		w.eq(WeappCustomizePage::getCompanyId, companyId).eq(WeappCustomizePage::getIsOpen, 1).eq(WeappCustomizePage::getPageType, "my");
		if (templateNameOrNull != null && !templateNameOrNull.isBlank()) {
			w.eq(WeappCustomizePage::getTemplateName, templateNameOrNull);
		}
		Long c = mapper.selectCount(w);
		return c == null ? 0L : c;
	}

	public long countOpenMyForCompanyExcludingId(long companyId, long excludePageId, String templateNameOrNull) {
		LambdaQueryWrapper<WeappCustomizePage> w = new LambdaQueryWrapper<>();
		w.eq(WeappCustomizePage::getCompanyId, companyId)
				.eq(WeappCustomizePage::getIsOpen, 1)
				.eq(WeappCustomizePage::getPageType, "my")
				.ne(WeappCustomizePage::getId, excludePageId);
		if (templateNameOrNull != null && !templateNameOrNull.isBlank()) {
			w.eq(WeappCustomizePage::getTemplateName, templateNameOrNull);
		}
		Long c = mapper.selectCount(w);
		return c == null ? 0L : c;
	}

	public int updateById(WeappCustomizePage entity) {
		return mapper.updateById(entity);
	}

	public void insertCustomizePage(WeappCustomizePage entity) {
		mapper.insert(entity);
	}

	public Optional<WeappCustomizePage> findById(long id) {
		return Optional.ofNullable(mapper.selectById(id));
	}

	public Optional<WeappCustomizePage> findByIdAndCompanyId(long id, long companyId) {
		LambdaQueryWrapper<WeappCustomizePage> w = new LambdaQueryWrapper<>();
		w.eq(WeappCustomizePage::getId, id).eq(WeappCustomizePage::getCompanyId, companyId).last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	public Optional<WeappCustomizePage> findOneByCompanyIdAndPageTypeAndTemplateName(long companyId, String pageType,
			String templateName) {
		LambdaQueryWrapper<WeappCustomizePage> w = new LambdaQueryWrapper<>();
		w.eq(WeappCustomizePage::getCompanyId, companyId)
				.eq(WeappCustomizePage::getPageType, pageType)
				.eq(WeappCustomizePage::getTemplateName, templateName)
				.last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	/**
	 * 前台「我的」自定义页：已开启的 {@code my} 页，按小镇优先级取一条。
	 */
	public Optional<WeappCustomizePage> findTopOpenMyPageForFront(long companyId, long regionauthId) {
		LambdaQueryWrapper<WeappCustomizePage> w = new LambdaQueryWrapper<>();
		w.eq(WeappCustomizePage::getCompanyId, companyId)
				.eq(WeappCustomizePage::getPageType, "my")
				.eq(WeappCustomizePage::getIsOpen, 1);
		if (regionauthId > 0L) {
			w.in(WeappCustomizePage::getRegionauthId, regionauthId, 0L)
					.orderByDesc(WeappCustomizePage::getRegionauthId)
					.orderByDesc(WeappCustomizePage::getId)
					.last("LIMIT 1");
		} else {
			w.eq(WeappCustomizePage::getRegionauthId, 0L).orderByDesc(WeappCustomizePage::getId).last("LIMIT 1");
		}
		return Optional.ofNullable(mapper.selectOne(w));
	}

	public Optional<WeappCustomizePage> findOneByIdCompanyIdAndRegionauthId(long id, long companyId, long regionauthId) {
		LambdaQueryWrapper<WeappCustomizePage> w = new LambdaQueryWrapper<>();
		w.eq(WeappCustomizePage::getId, id).eq(WeappCustomizePage::getCompanyId, companyId).eq(WeappCustomizePage::getRegionauthId, regionauthId)
				.last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	public int deleteByIdAndCompanyId(long id, long companyId) {
		LambdaQueryWrapper<WeappCustomizePage> w = new LambdaQueryWrapper<>();
		w.eq(WeappCustomizePage::getId, id).eq(WeappCustomizePage::getCompanyId, companyId);
		return mapper.delete(w);
	}

	public long countCustomizePageList(long companyId, boolean templateNameKeyPresent, String templateName, String pageType,
			boolean regionauthKeyPresent, long regionauthId) {
		LambdaQueryWrapper<WeappCustomizePage> w = customizePageListConditions(companyId, templateNameKeyPresent, templateName, pageType,
				regionauthKeyPresent, regionauthId);
		Long c = mapper.selectCount(w);
		return c == null ? 0L : c;
	}

	public List<WeappCustomizePage> pageCustomizePageList(long companyId, boolean templateNameKeyPresent, String templateName,
			String pageType, boolean regionauthKeyPresent, long regionauthId, int page, int pageSize) {
		LambdaQueryWrapper<WeappCustomizePage> w = customizePageListConditions(companyId, templateNameKeyPresent, templateName, pageType,
				regionauthKeyPresent, regionauthId);
		w.orderByDesc(WeappCustomizePage::getId);
		Page<WeappCustomizePage> mpPage = new Page<>(page, pageSize);
		return mapper.selectPage(mpPage, w).getRecords();
	}

	private static LambdaQueryWrapper<WeappCustomizePage> customizePageListConditions(long companyId, boolean templateNameKeyPresent,
			String templateName, String pageType, boolean regionauthKeyPresent, long regionauthId) {
		LambdaQueryWrapper<WeappCustomizePage> w = new LambdaQueryWrapper<>();
		w.eq(WeappCustomizePage::getCompanyId, companyId).eq(WeappCustomizePage::getPageType, pageType);
		if (!templateNameKeyPresent) {
			w.isNull(WeappCustomizePage::getTemplateName);
		} else {
			String t = templateName == null ? "" : templateName.trim();
			w.eq(WeappCustomizePage::getTemplateName, t);
		}
		if (regionauthKeyPresent) {
			w.eq(WeappCustomizePage::getRegionauthId, regionauthId);
		}
		return w;
	}
}
