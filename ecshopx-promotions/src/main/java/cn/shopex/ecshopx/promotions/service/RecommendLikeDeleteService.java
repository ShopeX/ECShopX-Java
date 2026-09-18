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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.promotions.domain.RecommendLike;
import cn.shopex.ecshopx.promotions.mapper.RecommendLikeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RecommendLikeDeleteService {

	private final RecommendLikeMapper recommendLikeMapper;

	public RecommendLikeDeleteService(RecommendLikeMapper recommendLikeMapper) {
		this.recommendLikeMapper = recommendLikeMapper;
	}

	public Map<String, Object> delRecommendLike(long companyId, String id) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		if ("all".equals(id)) {
			LambdaQueryWrapper<RecommendLike> wrapper =
					new LambdaQueryWrapper<RecommendLike>().eq(RecommendLike::getCompanyId, companyId);
			int rows = recommendLikeMapper.delete(wrapper);
			data.put("status", Integer.valueOf(rows));
			return data;
		}
		Long pk = parsePrimaryKey(id);
		if (pk == null) {
			data.put("status", Boolean.TRUE);
			return data;
		}
		recommendLikeMapper.deleteById(pk);
		data.put("status", Boolean.TRUE);
		return data;
	}

	/** 解析十进制长整型主键；空白或非法格式返回 {@code null}。 */
	private static Long parsePrimaryKey(String id) {
		if (id == null) {
			return null;
		}
		String s = id.trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
