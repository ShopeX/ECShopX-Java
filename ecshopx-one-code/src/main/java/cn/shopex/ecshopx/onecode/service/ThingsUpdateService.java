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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.onecode.domain.Things;
import cn.shopex.ecshopx.onecode.mapper.ThingsMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ThingsUpdateService {

	private final ThingsMapper thingsMapper;
	private final TransactionTemplate transactionTemplate;

	public ThingsUpdateService(ThingsMapper thingsMapper, PlatformTransactionManager transactionManager) {
		this.thingsMapper = thingsMapper;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public Map<String, Object> updateThings(
			long companyId,
			long thingId,
			String thingName,
			String pic,
			int priceInCents,
			boolean introKeyPresent,
			boolean introColumnUpdateRequested,
			String introValueIfUpdating) {
		Things byId = thingsMapper.selectById(thingId);
		if (byId == null) {
			throw new ResourceException("数据不存在");
		}
		if (byId.getCompanyId() == null || !Objects.equals(byId.getCompanyId(), companyId)) {
			throw new ResourceException("请确认您的物品信息后再提交.");
		}
		if (!introKeyPresent) {
			throw new BadRequestException("缺少必填字段: intro");
		}

		return transactionTemplate.execute(status -> {
			int now = (int) (System.currentTimeMillis() / 1000);
			var uw =
					Wrappers.<Things>lambdaUpdate()
							.eq(Things::getThingId, thingId)
							.eq(Things::getCompanyId, companyId)
							.set(Things::getThingName, thingName)
							.set(Things::getPic, pic)
							.set(Things::getPrice, priceInCents)
							.set(Things::getUpdated, now);
			if (introColumnUpdateRequested) {
				uw.set(Things::getIntro, introValueIfUpdating);
			}
			int rows = thingsMapper.update(null, uw);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			Things reloaded = thingsMapper.selectById(thingId);
			if (reloaded == null) {
				throw new ResourceException("未查询到更新数据");
			}
			return ThingsRowMapSupport.toThingRowMap(reloaded);
		});
	}
}
