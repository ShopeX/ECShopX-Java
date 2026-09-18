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

package cn.shopex.ecshopx.common.discount;

import java.util.Map;

/**
 * 运营端卡券明细聚合加载门面（由商品模块实现，供卡券 Controller 注入，避免 kaquan → goods 的 Maven 依赖）。
 */
public interface DiscountCardKaquanDetailLoadService {

	/**
	 * @param companyId 商户 ID（来自 JWT）
	 * @param cardIdRaw 已校验的 card_id 字符串（trim 后、整数格式）
	 * @param distributorIdRaw 可选店铺筛选，与库中 {@code ,id,} 存储格式匹配；空表示不按店铺过滤
	 */
	Map<String, Object> loadDetailForAdmin(long companyId, String cardIdRaw, String distributorIdRaw);
}
