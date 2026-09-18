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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.datacube.domain.Sources;
import cn.shopex.ecshopx.datacube.mapper.SourcesMapper;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourcesDeleteService {

	private final SourcesMapper sourcesMapper;

	public SourcesDeleteService(SourcesMapper sourcesMapper) {
		this.sourcesMapper = sourcesMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteSources(long companyId, long sourceId) {
		if (sourceId <= 0) {
			throw new ResourceException("来源id不能为空.");
		}
		Sources row = sourcesMapper.selectById(sourceId);
		if (row == null) {
			throw new ResourceException("source_id=" + sourceId + "的来源不存在");
		}
		if (!Objects.equals(companyId, row.getCompanyId())) {
			throw new ResourceException("删除来源信息有误.");
		}
		int n = sourcesMapper.deleteById(sourceId);
		if (n == 0) {
			throw new ResourceException("source_id=" + sourceId + "的来源不存在");
		}
	}
}
