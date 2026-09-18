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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PromoterGradeLevelUpdateService {

	private final PromoterMapper promoterMapper;

	public PromoterGradeLevelUpdateService(PromoterMapper promoterMapper) {
		this.promoterMapper = promoterMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void updatePromoterGrade(long companyId, String userIdRaw, String gradeLevelRaw) {
		String trimmed = userIdRaw == null ? "" : userIdRaw.trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new BadRequestException("参数错误");
		}
		long userId;
		try {
			userId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}
		if (userId <= 0L) {
			throw new BadRequestException("参数错误");
		}

		Promoter row = promoterMapper.selectOne(
				new LambdaQueryWrapper<Promoter>()
						.eq(Promoter::getUserId, userId)
						.last("LIMIT 1"));

		if (row == null) {
			throw new ResourceException("无效的推广员");
		}
		if (!Objects.equals(row.getIsPromoter(), 1)) {
			throw new ResourceException("无效的推广员");
		}
		if (!Objects.equals(row.getCompanyId(), companyId)) {
			throw new ResourceException("无效的推广员");
		}

		String s = gradeLevelRaw == null ? "" : gradeLevelRaw;
		long v = LeadingNumberParser.parseAsLong(s);
		int gradeLevelToStore;
		if (v > Integer.MAX_VALUE) {
			gradeLevelToStore = Integer.MAX_VALUE;
		} else if (v < Integer.MIN_VALUE) {
			gradeLevelToStore = Integer.MIN_VALUE;
		} else {
			gradeLevelToStore = (int) v;
		}

		int rows = promoterMapper.update(
				null,
				new LambdaUpdateWrapper<Promoter>()
						.eq(Promoter::getUserId, userId)
						.eq(Promoter::getCompanyId, companyId)
						.set(Promoter::getGradeLevel, gradeLevelToStore));
		if (rows != 1) {
			throw new ResourceException("更新失败");
		}
	}
}
