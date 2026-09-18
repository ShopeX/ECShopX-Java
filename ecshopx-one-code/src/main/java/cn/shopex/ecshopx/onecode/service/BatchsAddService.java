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
import cn.shopex.ecshopx.onecode.domain.Things;
import cn.shopex.ecshopx.onecode.mapper.BatchsMapper;
import cn.shopex.ecshopx.onecode.mapper.ThingsMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchsAddService {

	private final BatchsMapper batchsMapper;
	private final ThingsMapper thingsMapper;

	public BatchsAddService(BatchsMapper batchsMapper, ThingsMapper thingsMapper) {
		this.batchsMapper = batchsMapper;
		this.thingsMapper = thingsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> addBatchs(
			long companyId,
			long thingId,
			String batchNumber,
			String batchName,
			int batchQuantity,
			boolean showTraceForData,
			Object traceInfoRaw,
			boolean setTraceInfo) {
		int now = (int) (System.currentTimeMillis() / 1000);

		Batchs entity = new Batchs();
		entity.setCompanyId(companyId);
		entity.setThingId(thingId);
		entity.setBatchNumber(batchNumber);
		entity.setBatchName(batchName);
		entity.setBatchQuantity(batchQuantity);
		if (showTraceForData) {
			entity.setShowTrace(true);
		}
		if (setTraceInfo) {
			entity.setTraceInfo(traceInfoRaw);
		}
		entity.setCreated(now);
		entity.setUpdated(now);

		batchsMapper.insert(entity);

		Things things = thingsMapper.selectOne(
				Wrappers.<Things>lambdaQuery().eq(Things::getThingId, thingId));
		if (things == null) {
			throw new ResourceException("关联的码类型不存在");
		}
		int prevCount = defaultInt(things.getBatchTotalCount());
		int prevQty = defaultInt(things.getBatchTotalQuantity());
		int newBatchTotalCount = prevCount + 1;
		int newBatchTotalQuantity = prevQty + batchQuantity;

		int updated = thingsMapper.update(
				null,
				Wrappers.<Things>lambdaUpdate()
						.eq(Things::getThingId, thingId)
						.eq(Things::getCompanyId, companyId)
						.set(Things::getBatchTotalCount, newBatchTotalCount)
						.set(Things::getBatchTotalQuantity, newBatchTotalQuantity)
						.set(Things::getUpdated, now));
		if (updated == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		return BatchsRowMapSupport.toBatchRowMap(entity);
	}

	private static int defaultInt(Integer v) {
		return v == null ? 0 : v;
	}
}
