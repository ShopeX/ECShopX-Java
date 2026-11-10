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

package cn.shopex.ecshopx.goods.search.index;

/**
 * 商品搜索 ES 文档 {@code _id}：一店一品一条。
 */
public final class GoodsSearchDocumentId {

	private GoodsSearchDocumentId() {}

	public static String of(long companyId, long distributorId, long defaultItemId) {
		return companyId + "_" + distributorId + "_" + defaultItemId;
	}
}
