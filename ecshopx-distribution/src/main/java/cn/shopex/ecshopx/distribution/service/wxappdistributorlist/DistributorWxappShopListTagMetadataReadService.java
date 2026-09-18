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

package cn.shopex.ecshopx.distribution.service.wxappdistributorlist;

import cn.shopex.ecshopx.distribution.domain.DistributorTags;
import cn.shopex.ecshopx.distribution.mapper.DistributorTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DistributorWxappShopListTagMetadataReadService {

	private static final String REDIS_PREFIX = "distributor_tags:";

	private final DistributorTagsMapper distributorTagsMapper;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public DistributorWxappShopListTagMetadataReadService(
			DistributorTagsMapper distributorTagsMapper,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.distributorTagsMapper = distributorTagsMapper;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> listFrontShowTagRows(long companyId) {
		String key = REDIS_PREFIX + companyId;
		String cached = stringRedisTemplate.opsForValue().get(key);
		if (cached != null) {
			try {
				return objectMapper.readValue(cached, new TypeReference<List<Map<String, Object>>>() {});
			} catch (Exception ignored) {
				// fall through to DB
			}
		}
		List<DistributorTags> rows =
				distributorTagsMapper.selectList(
						new LambdaQueryWrapper<DistributorTags>()
								.eq(DistributorTags::getCompanyId, companyId)
								.eq(DistributorTags::getFrontShow, Integer.valueOf(1)));
		List<Map<String, Object>> out = new ArrayList<>();
		for (DistributorTags r : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("tag_id", r.getTagId());
			m.put("tag_name", r.getTagName());
			m.put("tag_color", r.getTagColor());
			m.put("font_color", r.getFontColor());
			m.put("description", r.getDescription());
			m.put("tag_icon", r.getTagIcon());
			out.add(m);
		}
		try {
			stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(out), Duration.ofSeconds(60L));
		} catch (Exception ignored) {
			// ignore cache failures
		}
		return out;
	}
}
