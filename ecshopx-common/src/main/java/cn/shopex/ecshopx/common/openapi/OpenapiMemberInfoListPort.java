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

import java.util.Map;

public interface OpenapiMemberInfoListPort {

	/**
	 * @return 无 association 匹配时为 {@code List}（空数组）；否则为 {@code Map{count,list}}
	 */
	Object memberInfoList(
			long companyId,
			String unionidQueryParam,
			Map<String, Object> body,
			boolean unionidPresent,
			String unionidRaw,
			int associationsPage,
			Integer associationsPageSize);
}
