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
import cn.shopex.ecshopx.members.domain.MemberArticleFav;
import cn.shopex.ecshopx.members.mapper.MemberArticleFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MemberArticleFavGetInfoService {

	private final MemberArticleFavMapper memberArticleFavMapper;
	private final ArticleDetailQueryService articleDetailQueryService;

	public MemberArticleFavGetInfoService(
			MemberArticleFavMapper memberArticleFavMapper,
			ArticleDetailQueryService articleDetailQueryService) {
		this.memberArticleFavMapper = memberArticleFavMapper;
		this.articleDetailQueryService = articleDetailQueryService;
	}

	public Object getArticleFavInfo(long companyId, long userId, String articleIdRaw, String requestLang) {
		long parsedArticleId;
		try {
			String t = articleIdRaw == null ? "" : articleIdRaw.trim();
			parsedArticleId = Long.parseLong(t);
		} catch (NumberFormatException e) {
			return Collections.emptyList();
		}
		if (parsedArticleId <= 0L) {
			return Collections.emptyList();
		}

		MemberArticleFav fav =
				memberArticleFavMapper.selectOne(
						new LambdaQueryWrapper<MemberArticleFav>()
								.eq(MemberArticleFav::getCompanyId, companyId)
								.eq(MemberArticleFav::getUserId, userId)
								.eq(MemberArticleFav::getArticleId, parsedArticleId));
		if (fav == null) {
			return Collections.emptyList();
		}

		Long favCompanyId = fav.getCompanyId();
		Long favArticleId = fav.getArticleId();
		if (favCompanyId == null
				|| favArticleId == null
				|| favCompanyId <= 0L
				|| favArticleId <= 0L) {
			return Collections.emptyList();
		}

		Optional<Map<String, Object>> opt =
				articleDetailQueryService.findArticleDetailRowForMemberFav(
						favCompanyId.longValue(), favArticleId.longValue(), requestLang);
		if (opt.isEmpty()) {
			return Collections.emptyList();
		}
		return opt.get();
	}
}
