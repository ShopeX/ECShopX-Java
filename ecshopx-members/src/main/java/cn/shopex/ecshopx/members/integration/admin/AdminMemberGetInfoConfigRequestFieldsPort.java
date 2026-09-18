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

package cn.shopex.ecshopx.members.integration.admin;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public interface AdminMemberGetInfoConfigRequestFieldsPort {

	void enrichMemberInfo(long companyId, Map<String, Object> memberRow, HttpServletRequest request);

	/**
	 * Batch variant of {@link #enrichMemberInfo(long, Map, HttpServletRequest)}: loads field config once
	 * for the company, then applies it to every row.
	 */
	void enrichMemberInfoList(
			long companyId, List<? extends Map<String, Object>> memberRows, HttpServletRequest request);
}
