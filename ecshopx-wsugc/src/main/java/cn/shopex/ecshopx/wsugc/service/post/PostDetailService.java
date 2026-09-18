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

package cn.shopex.ecshopx.wsugc.service.post;

import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PostDetailService {

	private final PostMapper postMapper;
	private final PostOutsideLangReadService postOutsideLangReadService;
	private final PostAdminRowFormatService postAdminRowFormatService;

	public PostDetailService(
			PostMapper postMapper,
			PostOutsideLangReadService postOutsideLangReadService,
			PostAdminRowFormatService postAdminRowFormatService) {
		this.postMapper = postMapper;
		this.postOutsideLangReadService = postOutsideLangReadService;
		this.postAdminRowFormatService = postAdminRowFormatService;
	}

	public Map<String, Object> buildResponse(
			String postIdRaw, Map<String, Object> operatorJwt, String requestLangTag) {
		long companyId = readLong(operatorJwt.get("company_id"), 1L);
		Long postId = parseOptionalLong(postIdRaw);
		if (postId == null) {
			return sortedOuter(null);
		}
		LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<>();
		w.eq(Post::getCompanyId, companyId).eq(Post::getPostId, postId).last("LIMIT 1");
		Post row = postMapper.selectOne(w);
		if (row == null) {
			return sortedOuter(null);
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>(PostCreateService.postToSnakeMap(row));
		postOutsideLangReadService.applyToRowMap(companyId, requestLangTag, m);
		postAdminRowFormatService.formatAdminRow(
				m, row, companyId, requestLangTag, PostAdminRowFormatService.Mode.POST_DETAIL);
		return sortedOuter(ksortCopy(m));
	}

	private static Long parseOptionalLong(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long readLong(Object v, long defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static Map<String, Object> sortedOuter(Map<String, Object> postInfoOrNull) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("post_info", postInfoOrNull);
		return ksortCopy(out);
	}

	private static LinkedHashMap<String, Object> ksortCopy(Map<String, Object> src) {
		TreeMap<String, Object> sorted = new TreeMap<>(src);
		return new LinkedHashMap<>(sorted);
	}
}
