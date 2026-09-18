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
import cn.shopex.ecshopx.companys.domain.Article;
import cn.shopex.ecshopx.companys.mapper.ArticleMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ArticleDeleteService {

	private static final String TABLE_LANG = "companys_article";
	private static final String MODULE_LANG = "companys_article";

	private final ArticleMapper articleMapper;
	private final CommonLangModWriteService commonLangModWriteService;
	private final LangueProperties langueProperties;

	public ArticleDeleteService(
			ArticleMapper articleMapper,
			CommonLangModWriteService commonLangModWriteService,
			LangueProperties langueProperties) {
		this.articleMapper = articleMapper;
		this.commonLangModWriteService = commonLangModWriteService;
		this.langueProperties = langueProperties;
	}

	private void deleteAllLangForArticle(long companyId, long dataId) {
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
	public void deleteDataArticle(String articleIdPath, long companyId) {
		String p = articleIdPath == null ? "" : articleIdPath.trim();
		if (p.isEmpty()) {
			throw new ResourceException("删除的数据不存在");
		}
		long articleId;
		try {
			articleId = Long.parseLong(p);
		} catch (NumberFormatException e) {
			throw new ResourceException("删除的数据不存在");
		}
		if (articleId <= 0L) {
			throw new ResourceException("删除的数据不存在");
		}

		final LambdaQueryWrapper<Article> baseWrapper =
				new LambdaQueryWrapper<Article>()
						.eq(Article::getCompanyId, companyId)
						.eq(Article::getArticleId, articleId);

		int pageNo = 1;
		final int pageSize = 500;
		while (true) {
			Page<Article> page = new Page<>(pageNo, pageSize);
			Page<Article> result = articleMapper.selectPage(page, baseWrapper);
			if (result.getRecords().isEmpty()) {
				break;
			}
			for (Article row : result.getRecords()) {
				if (row.getArticleId() == null) {
					throw new ResourceException("删除的数据不存在");
				}
				deleteAllLangForArticle(companyId, row.getArticleId());
			}
			if (result.getRecords().size() < pageSize) {
				break;
			}
			pageNo++;
		}

		int removed = articleMapper.delete(baseWrapper);
		if (removed == 0) {
			throw new ResourceException("删除的数据不存在");
		}
	}
}
