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

import cn.shopex.ecshopx.members.domain.MemberArticleFav;
import cn.shopex.ecshopx.members.mapper.MemberArticleFavMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MemberArticleFavAddService {

	private final MemberArticleFavMapper memberArticleFavMapper;

	public MemberArticleFavAddService(MemberArticleFavMapper memberArticleFavMapper) {
		this.memberArticleFavMapper = memberArticleFavMapper;
	}

	public Map<String, Object> addArticleFav(long companyId, long userId, long articleId) {
		LambdaQueryWrapper<MemberArticleFav> q = new LambdaQueryWrapper<>();
		q.eq(MemberArticleFav::getCompanyId, companyId)
				.eq(MemberArticleFav::getUserId, userId)
				.eq(MemberArticleFav::getArticleId, articleId)
				.last("LIMIT 1");
		MemberArticleFav existing = memberArticleFavMapper.selectOne(q);
		if (existing != null) {
			return toFavRowMap(existing);
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		MemberArticleFav row = new MemberArticleFav();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setArticleId(articleId);
		row.setCreated(nowSec);
		memberArticleFavMapper.insert(row);
		return toFavRowMap(row);
	}

	private static Map<String, Object> toFavRowMap(MemberArticleFav e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("fav_id", e.getFavId());
		m.put("company_id", e.getCompanyId());
		m.put("user_id", e.getUserId());
		m.put("article_id", e.getArticleId());
		m.put("created", e.getCreated());
		return m;
	}
}
