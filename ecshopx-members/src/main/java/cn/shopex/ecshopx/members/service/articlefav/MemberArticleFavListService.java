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

package cn.shopex.ecshopx.members.service.articlefav;

import cn.shopex.ecshopx.companys.service.article.ArticleDetailQueryService;
import cn.shopex.ecshopx.companys.service.article.ArticleListQueryService;
import cn.shopex.ecshopx.members.domain.MemberArticleFav;
import cn.shopex.ecshopx.members.mapper.MemberArticleFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberArticleFavListService {

	private final MemberArticleFavMapper memberArticleFavMapper;
	private final ArticleListQueryService articleListQueryService;
	private final ArticleDetailQueryService articleDetailQueryService;

	public MemberArticleFavListService(
			MemberArticleFavMapper memberArticleFavMapper,
			ArticleListQueryService articleListQueryService,
			ArticleDetailQueryService articleDetailQueryService) {
		this.memberArticleFavMapper = memberArticleFavMapper;
		this.articleListQueryService = articleListQueryService;
		this.articleDetailQueryService = articleDetailQueryService;
	}

	public Object getArticleFavList(long companyId, long userId, int page, int pageSize, String requestLang) {
		LambdaQueryWrapper<MemberArticleFav> w = new LambdaQueryWrapper<>();
		w.eq(MemberArticleFav::getCompanyId, companyId)
				.eq(MemberArticleFav::getUserId, userId)
				.orderByDesc(MemberArticleFav::getFavId);
		Page<MemberArticleFav> mp = new Page<>(page, pageSize);
		memberArticleFavMapper.selectPage(mp, w);
		List<MemberArticleFav> records = mp.getRecords();
		if (records == null || records.isEmpty()) {
			return Collections.emptyList();
		}

		List<Long> ids = new ArrayList<>();
		for (int i = records.size() - 1; i >= 0; i--) {
			Long aid = records.get(i).getArticleId();
			if (aid != null) {
				ids.add(aid);
			}
		}
		if (ids.isEmpty()) {
			return Collections.emptyList();
		}

		Map<String, Object> articlePayload =
				articleListQueryService.listArticlesByIdsForMemberFav(ids, requestLang);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list =
				(List<Map<String, Object>>) articlePayload.get("list");
		if (list == null) {
			list = List.of();
		}

		for (Map<String, Object> row : list) {
			long cid = ((Number) row.get("company_id")).longValue();
			long aid = ((Number) row.get("article_id")).longValue();
			Map<String, Object> chk =
					articleDetailQueryService.articlePraiseCheck(cid, String.valueOf(aid), userId);
			row.put("isPraise", Boolean.TRUE.equals(chk.get("status")));
		}

		return articlePayload;
	}
}
