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

package cn.shopex.ecshopx.common.port.shuyun;

import java.util.Map;

/**
 * 数云线下权益发券（C4/C5）。shuyun 注入；kaquan 提供真实实现。
 * 对齐 PHP {@code ShuyunOfflineBenefitCouponGrantServiceInterface}。
 */
public interface ShuyunOfflineBenefitCouponGrantPort {

	/**
	 * 按券模板发券。
	 *
	 * @return 至少含 {@code code} 券码
	 */
	Map<String, Object> grantByCardTemplate(long companyId, long cardId, long userId, String sourceFrom);
}
