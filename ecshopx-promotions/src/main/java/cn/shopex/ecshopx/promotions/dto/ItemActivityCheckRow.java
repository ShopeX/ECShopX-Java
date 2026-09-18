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

package cn.shopex.ecshopx.promotions.dto;

import java.util.List;
import lombok.Data;

/**
 * 商品维度（用于营销/限购冲突校验）。
 */
@Data
public class ItemActivityCheckRow {

	private long itemId;

	private Long mainCatId;

	private Long brandId;

	/**
	 * 请求中的全部商品 ID，与限购/营销 SQL 中 {@code item_type = 'normal'} 的 IN 条件一致；各行应携带相同引用。
	 */
	private List<Long> allRequestItemIds;
}
