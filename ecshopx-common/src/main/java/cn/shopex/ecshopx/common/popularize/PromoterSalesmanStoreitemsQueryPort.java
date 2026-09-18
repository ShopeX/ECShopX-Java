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

package cn.shopex.ecshopx.common.popularize;

/** H5 推广员「业务员关联店铺商品 + 店铺列表」查询端口。 */
public interface PromoterSalesmanStoreitemsQueryPort {

	Object getSalesmanStoreitems(
			long companyId,
			long userId,
			int page,
			int pageSize,
			Object isDefaultRaw,
			String distributorIdRaw,
			String requestLangOrCountryCode);
}
