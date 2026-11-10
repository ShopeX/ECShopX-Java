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

package cn.shopex.ecshopx.goods.search.synonym;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.GoodsSearchSynonym;
import cn.shopex.ecshopx.goods.mapper.SearchSynonymMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class SearchSynonymRepository {

	private final SearchSynonymMapper mapper;
	private final SearchSynonymWordsCodec wordsCodec;

	public SearchSynonymRepository(SearchSynonymMapper mapper, SearchSynonymWordsCodec wordsCodec) {
		this.mapper = mapper;
		this.wordsCodec = wordsCodec;
	}

	public List<GoodsSearchSynonym> listByCompanyId(long companyId) {
		LambdaQueryWrapper<GoodsSearchSynonym> w = new LambdaQueryWrapper<>();
		w.eq(GoodsSearchSynonym::getCompanyId, companyId).orderByAsc(GoodsSearchSynonym::getGroupKey);
		return mapper.selectList(w);
	}

	public GoodsSearchSynonym replaceGroup(long companyId, String groupKey, List<String> words) {
		String key = requireGroupKey(groupKey);
		List<String> normalized = SearchSynonymWordsCodec.normalize(words);
		if (normalized.isEmpty()) {
			throw new BadRequestException("words 不能为空");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);

		GoodsSearchSynonym existing = selectByCompanyAndGroupKey(companyId, key);
		if (existing == null) {
			GoodsSearchSynonym entity = new GoodsSearchSynonym();
			entity.setCompanyId(companyId);
			entity.setGroupKey(key);
			entity.setWords(wordsCodec.encode(normalized));
			entity.setCreated(now);
			entity.setUpdated(now);
			mapper.insert(entity);
			return entity;
		}
		existing.setWords(wordsCodec.encode(normalized));
		existing.setUpdated(now);
		int rows = mapper.updateById(existing);
		if (rows == 0) {
			throw new ResourceException("更新同义词失败");
		}
		return existing;
	}

	public void deleteByCompanyAndId(long companyId, long id) {
		if (id <= 0L) {
			throw new BadRequestException("id 无效");
		}
		LambdaQueryWrapper<GoodsSearchSynonym> w = new LambdaQueryWrapper<>();
		w.eq(GoodsSearchSynonym::getCompanyId, companyId).eq(GoodsSearchSynonym::getId, id);
		int rows = mapper.delete(w);
		if (rows == 0) {
			throw new ResourceException("同义词组不存在");
		}
	}

	GoodsSearchSynonym selectByCompanyAndGroupKey(long companyId, String groupKey) {
		LambdaQueryWrapper<GoodsSearchSynonym> w = new LambdaQueryWrapper<>();
		w.eq(GoodsSearchSynonym::getCompanyId, companyId).eq(GoodsSearchSynonym::getGroupKey, groupKey).last("LIMIT 1");
		return mapper.selectOne(w);
	}

	private static String requireGroupKey(String groupKey) {
		if (!StringUtils.hasText(groupKey)) {
			throw new BadRequestException("group_key 必填");
		}
		String key = groupKey.trim();
		if (key.length() > 64) {
			throw new BadRequestException("group_key 过长");
		}
		return key;
	}
}
