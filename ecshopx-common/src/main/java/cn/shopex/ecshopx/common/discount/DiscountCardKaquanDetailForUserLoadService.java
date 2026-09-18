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
 * H5 用户路径加载卡券模板全量详情（由商品模块实现，供卡券模块注入，避免 kaquan → goods 的 Maven 依赖）。
 */
public interface DiscountCardKaquanDetailForUserLoadService {

	/**
	 * 按公司 + 模板卡 ID 加载详情；不按管理端店铺行过滤。
	 */
	Map<String, Object> loadDetailForUserCard(long companyId, long cardId);
}
