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

package cn.shopex.ecshopx.goods.repository;

import java.util.List;
import lombok.Data;

/**
 * 商品标签列表查询条件（与 {@link ItemsTagsRepository#selectPageByFilter} 配套）。
 */
@Data
public class ItemsTagsListFilter {

	public enum DistributorMode {
		EQ,
		GT_ZERO,
		IN
	}

	private long companyId;
	private List<Long> tagIdsIn;
	private DistributorMode distributorMode;
	private Long distributorEq;
	private List<Long> distributorIn;
	private String tagNameContains;
	private boolean frontShowSpecified;
	private Integer frontShowValue;
}
