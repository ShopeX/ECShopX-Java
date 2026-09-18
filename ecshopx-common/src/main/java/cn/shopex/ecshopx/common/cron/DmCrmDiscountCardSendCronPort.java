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

package cn.shopex.ecshopx.common.cron;

/**
 * 计划活动 test-cron 侧达摩发券窄 Port（与 {@code DmCrmDiscountCardSendPort} 对齐），避免 common 依赖 third-party 模块。
 */
public interface DmCrmDiscountCardSendCronPort {

	boolean isOpen(long companyId);

	/** @return dm_card_code; empty string when not applicable or skipped */
	String sendCoupon(long companyId, long userId, long cardId, String dmCardId, String mobile);
}
