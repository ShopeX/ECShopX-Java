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
import cn.shopex.ecshopx.onecode.domain.Things;
import cn.shopex.ecshopx.onecode.mapper.ThingsMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThingsDeleteService {

	private final ThingsMapper thingsMapper;

	public ThingsDeleteService(ThingsMapper thingsMapper) {
		this.thingsMapper = thingsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteThings(long companyId, long thingId) {
		if (thingId < 1) {
			throw new ResourceException("物品id不能为空.");
		}
		Things row = thingsMapper.selectById(thingId);
		if (row == null) {
			throw new ResourceException("数据不存在");
		}
		Long cid = row.getCompanyId();
		if (cid == null || cid.longValue() != companyId) {
			throw new ResourceException("删除物品信息有误.");
		}
		int rows =
				thingsMapper.delete(
						Wrappers.<Things>lambdaQuery()
								.eq(Things::getThingId, thingId)
								.eq(Things::getCompanyId, companyId));
		if (rows == 0) {
			throw new ResourceException("删除的数据不存在");
		}
	}
}
