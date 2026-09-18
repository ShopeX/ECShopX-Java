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

package cn.shopex.ecshopx.goods.service.keywords;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Keywords;
import cn.shopex.ecshopx.goods.mapper.KeywordsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoodsKeywordsWriteService {

	private final KeywordsMapper keywordsMapper;

	public GoodsKeywordsWriteService(KeywordsMapper keywordsMapper) {
		this.keywordsMapper = keywordsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> addKeywords(long companyId, Long id, long distributorId, String content) {
		if (id != null) {
			Keywords existing = keywordsMapper.selectById(id);
			if (existing == null) {
				throw new ResourceException("记录不存在");
			}
			Keywords entity = keywordsMapper.selectById(id);
			if (entity == null) {
				throw new ResourceException("未查询到更新数据");
			}
			if (companyId != 0L) {
				entity.setCompanyId(companyId);
			}
			if (distributorId != 0L) {
				entity.setDistributorId(distributorId);
			}
			entity.setContent(content);
			int rows = keywordsMapper.updateById(entity);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			Keywords refreshed = keywordsMapper.selectById(id);
			if (refreshed == null) {
				throw new ResourceException("未查询到更新数据");
			}
			return toRow(refreshed);
		}

		Keywords entity = new Keywords();
		if (companyId != 0L) {
			entity.setCompanyId(companyId);
		}
		if (distributorId != 0L) {
			entity.setDistributorId(distributorId);
		}
		entity.setContent(content);
		keywordsMapper.insert(entity);
		return toRow(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteByCompanyAndKeywordId(long companyId, long keywordId) {
		LambdaQueryWrapper<Keywords> wrapper = new LambdaQueryWrapper<Keywords>()
				.eq(Keywords::getCompanyId, companyId)
				.eq(Keywords::getId, keywordId);
		keywordsMapper.delete(wrapper);
	}

	private static Map<String, Object> toRow(Keywords k) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", k.getId());
		m.put("company_id", k.getCompanyId());
		Long dist = k.getDistributorId();
		m.put("distributor_id", dist != null ? dist : 0L);
		m.put("content", k.getContent());
		return m;
	}
}
