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

package cn.shopex.ecshopx.common.openapi;

public interface OpenapiItemMainCategoryDetailPort {

	/**
	 * 主类目详情：正常命中返回 {@code Map<String, Object>}；步骤 3.5 复核失败返回空列表（信封 data:[]）。
	 */
	Object getItemMainCategoryDetail(long companyId, String categoryRaw);
}
