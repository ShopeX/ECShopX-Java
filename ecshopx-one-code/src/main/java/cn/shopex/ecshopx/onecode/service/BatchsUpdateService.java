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
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.onecode.domain.Batchs;
import cn.shopex.ecshopx.onecode.mapper.BatchsMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchsUpdateService {

	private final BatchsMapper batchsMapper;

	public BatchsUpdateService(BatchsMapper batchsMapper) {
		this.batchsMapper = batchsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateBatchs(
			long companyId,
			long batchId,
			long thingId,
			String batchNumber,
			String batchName,
			int batchQuantity,
			boolean persistShowTraceColumn,
			Object traceInfoRaw,
			boolean traceInfoKeyPresent) {
		Batchs existing = batchsMapper.selectById(batchId);
		if (existing == null) {
			throw new ResourceException("数据不存在");
		}
		if (existing.getCompanyId() == null || existing.getCompanyId() != companyId) {
			throw new ResourceException("请确认您的物品批次信息后再提交.");
		}

		int now = (int) (System.currentTimeMillis() / 1000);

		var uw =
				Wrappers.<Batchs>lambdaUpdate()
						.eq(Batchs::getBatchId, batchId)
						.eq(Batchs::getCompanyId, companyId)
						.set(Batchs::getThingId, thingId)
						.set(Batchs::getBatchNumber, batchNumber)
						.set(Batchs::getBatchName, batchName)
						.set(Batchs::getBatchQuantity, batchQuantity)
						.set(Batchs::getUpdated, now);
		if (persistShowTraceColumn) {
			uw.set(Batchs::getShowTrace, true);
		}
		if (traceInfoKeyPresent && ValuePresence.hasEffectiveValue(traceInfoRaw)) {
			uw.set(Batchs::getTraceInfo, traceInfoRaw);
		}

		int rows = batchsMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		Batchs reloaded = batchsMapper.selectById(batchId);
		if (reloaded == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return BatchsRowMapSupport.toBatchRowMap(reloaded);
	}

}
