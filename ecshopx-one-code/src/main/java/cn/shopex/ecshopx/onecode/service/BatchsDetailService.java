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

package cn.shopex.ecshopx.onecode.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.onecode.domain.Batchs;
import cn.shopex.ecshopx.onecode.mapper.BatchsMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BatchsDetailService {

	private final BatchsMapper batchsMapper;

	public BatchsDetailService(BatchsMapper batchsMapper) {
		this.batchsMapper = batchsMapper;
	}

	public Map<String, Object> getDetail(long operatorCompanyId, long batchId) {
		Batchs entity = batchsMapper.selectById(batchId);
		if (entity == null) {
			throw new ResourceException("数据不存在");
		}
		Long cid = entity.getCompanyId();
		if (cid == null || cid.longValue() != operatorCompanyId) {
			throw new ResourceException("获取物品批次信息有误，请确认物品批次ID.", 422);
		}
		return BatchsRowMapSupport.toBatchRowMap(entity);
	}
}
