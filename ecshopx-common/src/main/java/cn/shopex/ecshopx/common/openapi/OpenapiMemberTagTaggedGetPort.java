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

import java.util.List;
import java.util.Map;

public interface OpenapiMemberTagTaggedGetPort {

	/**
	 * @return 有标签时非空 List；无标签时 {@code null}（对齐 PHP 空数组 → data:null）
	 */
	List<Map<String, Object>> getMemberTagged(long companyId, String mobileRaw);
}
