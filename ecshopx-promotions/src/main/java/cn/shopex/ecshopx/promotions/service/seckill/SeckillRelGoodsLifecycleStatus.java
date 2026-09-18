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

package cn.shopex.ecshopx.promotions.service.seckill;

import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;

/** Derives rel-row lifecycle status from epoch timestamps only (no disabled flag). */
public final class SeckillRelGoodsLifecycleStatus {

	private SeckillRelGoodsLifecycleStatus() {}

	public static String compute(SeckillRelGoods e, int nowEpochSec) {
		int end = nz(e.getActivityEndTime());
		int start = nz(e.getActivityStartTime());
		int release = nz(e.getActivityReleaseTime());
		if (nowEpochSec >= end) {
			return "it_has_ended";
		}
		if (nowEpochSec >= start && nowEpochSec < end) {
			return "in_sale";
		}
		if (nowEpochSec >= release && nowEpochSec < start) {
			return "in_the_notice";
		}
		if (nowEpochSec < release) {
			return "waiting";
		}
		return "it_has_ended";
	}

	private static int nz(Integer t) {
		return t == null ? 0 : t;
	}
}
