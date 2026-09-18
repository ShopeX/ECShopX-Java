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
public class PromoterShopStatusUpdateService {

	private final PromoterMapper promoterMapper;

	public PromoterShopStatusUpdateService(PromoterMapper promoterMapper) {
		this.promoterMapper = promoterMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void updatePromoterShop(long companyId, String userIdRaw, String statusRaw, String reasonRaw) {
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

		long v = LeadingNumberParser.parseAsLong(statusRaw == null ? "" : statusRaw);
		int statusInt;
		if (v > Integer.MAX_VALUE) {
			statusInt = Integer.MAX_VALUE;
		} else if (v < Integer.MIN_VALUE) {
			statusInt = Integer.MIN_VALUE;
		} else {
			statusInt = (int) v;
		}

		Promoter promoter =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getCompanyId, companyId)
								.eq(Promoter::getUserId, userId)
								.eq(Promoter::getIsPromoter, 1)
								.eq(Promoter::getDisabled, 0)
								.last("LIMIT 1"));

		if (promoter == null) {
			throw new ResourceException("当前推广员无权限");
		}

		if (statusInt == 2 && Objects.equals(promoter.getShopStatus(), 1)) {
			return;
		}

		LambdaUpdateWrapper<Promoter> uw =
				new LambdaUpdateWrapper<Promoter>()
						.eq(Promoter::getId, promoter.getId())
						.set(Promoter::getShopStatus, statusInt);
		String r = reasonRaw == null ? "" : reasonRaw.trim();
		if (StringUtils.hasText(r)) {
			uw.set(Promoter::getReason, r);
		}
		promoterMapper.update(null, uw);
	}
}
