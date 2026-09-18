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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.orders.domain.Rights;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/** 小程序权益列表查询条件（本接口专用）。 */
public final class RightsWxappListQuerySupport {

	private RightsWxappListQuerySupport() {}

	private static void applyCommonFilter(
			LambdaQueryWrapper<Rights> w,
			long companyId,
			long userId,
			int endTimeGtExclusive,
			Integer validFilter) {
		w.eq(Rights::getCompanyId, companyId);
		w.eq(Rights::getUserId, userId);
		w.gt(Rights::getEndTime, endTimeGtExclusive);
		if (validFilter != null) {
			if (validFilter == 1) {
				w.eq(Rights::getStatus, "valid");
			} else {
				w.ne(Rights::getStatus, "valid");
			}
		}
	}

	public static LambdaQueryWrapper<Rights> forCount(
			long companyId, long userId, int endTimeGtExclusive, Integer validFilter) {
		LambdaQueryWrapper<Rights> w = new LambdaQueryWrapper<>();
		applyCommonFilter(w, companyId, userId, endTimeGtExclusive, validFilter);
		return w;
	}

	public static LambdaQueryWrapper<Rights> forPage(
			long companyId, long userId, int endTimeGtExclusive, Integer validFilter) {
		LambdaQueryWrapper<Rights> w = new LambdaQueryWrapper<>();
		applyCommonFilter(w, companyId, userId, endTimeGtExclusive, validFilter);
		w.orderByAsc(Rights::getEndTime);
		return w;
	}
}
