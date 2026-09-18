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

package cn.shopex.ecshopx.reservation.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.reservation.domain.DefaultWorkShift;
import cn.shopex.ecshopx.reservation.mapper.DefaultWorkShiftMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultWorkShiftDeleteService {

	private final DefaultWorkShiftMapper defaultWorkShiftMapper;

	public DefaultWorkShiftDeleteService(DefaultWorkShiftMapper defaultWorkShiftMapper) {
		this.defaultWorkShiftMapper = defaultWorkShiftMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteDefaultWorkShift(long companyId, long shopId) {
		DefaultWorkShift row =
				defaultWorkShiftMapper.selectOne(
						Wrappers.<DefaultWorkShift>lambdaQuery()
								.eq(DefaultWorkShift::getCompanyId, companyId)
								.eq(DefaultWorkShift::getShopId, shopId));
		if (row == null) {
			throw new ResourceException("修改默认排班不存在");
		}
		int deleted =
				defaultWorkShiftMapper.delete(
						Wrappers.<DefaultWorkShift>lambdaQuery()
								.eq(DefaultWorkShift::getCompanyId, companyId)
								.eq(DefaultWorkShift::getShopId, shopId));
		if (deleted == 0) {
			throw new ResourceException("修改默认排班不存在");
		}
	}
}
