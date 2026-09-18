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

package cn.shopex.ecshopx.companys.service.article;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.domain.ArticleCategory;
import cn.shopex.ecshopx.companys.mapper.ArticleCategoryMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ArticleCategoryDeleteService {

	private static final String TABLE_LANG = "companys_article_category";
	private static final String MODULE_LANG = "companys_article_category";

	private final ArticleCategoryMapper articleCategoryMapper;
	private final CommonLangModWriteService commonLangModWriteService;
	private final LangueProperties langueProperties;

	public ArticleCategoryDeleteService(
			ArticleCategoryMapper articleCategoryMapper,
			CommonLangModWriteService commonLangModWriteService,
			LangueProperties langueProperties) {
		this.articleCategoryMapper = articleCategoryMapper;
		this.commonLangModWriteService = commonLangModWriteService;
		this.langueProperties = langueProperties;
	}

	private void deleteAllLangForArticleCategory(long companyId, long dataId) {
		List<String> langs = langueProperties.getList();
		if (langs == null) {
			return;
		}
		for (String lang : langs) {
			if (StringUtils.hasText(lang)) {
				commonLangModWriteService.deleteLang((int) companyId, TABLE_LANG, dataId, MODULE_LANG, lang);
			}
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteCategory(long categoryId, long companyId) {
		LambdaQueryWrapper<ArticleCategory> anyChildProbe =
				new LambdaQueryWrapper<ArticleCategory>().eq(ArticleCategory::getParentId, categoryId);
		long anyCount = articleCategoryMapper.selectCount(anyChildProbe);
		boolean hasAnyChildGlobally = anyCount > 0L;

		LambdaQueryWrapper<ArticleCategory> targetW =
				new LambdaQueryWrapper<ArticleCategory>()
						.eq(ArticleCategory::getCategoryId, categoryId)
						.eq(ArticleCategory::getCompanyId, companyId);
		ArticleCategory targetRow = articleCategoryMapper.selectOne(targetW);
		if (targetRow != null) {
			deleteAllLangForArticleCategory(companyId, categoryId);
		}
		articleCategoryMapper.delete(targetW);

		if (!hasAnyChildGlobally) {
			return;
		}

		LambdaQueryWrapper<ArticleCategory> childrenW =
				new LambdaQueryWrapper<ArticleCategory>()
						.eq(ArticleCategory::getParentId, categoryId)
						.eq(ArticleCategory::getCompanyId, companyId);
		List<ArticleCategory> children = articleCategoryMapper.selectList(childrenW);
		for (ArticleCategory row : children) {
			Long cid = row.getCategoryId();
			if (cid == null) {
				throw new ResourceException("文章栏目数据异常：子节点 category_id 为空");
			}
			deleteAllLangForArticleCategory(companyId, cid.longValue());
			articleCategoryMapper.delete(
					new LambdaQueryWrapper<ArticleCategory>()
							.eq(ArticleCategory::getCategoryId, cid)
							.eq(ArticleCategory::getCompanyId, companyId));
		}
	}
}
