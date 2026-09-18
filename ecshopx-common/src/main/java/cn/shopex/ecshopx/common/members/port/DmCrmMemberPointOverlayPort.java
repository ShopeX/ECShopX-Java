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

package cn.shopex.ecshopx.common.members.port;

import java.util.Map;

public interface DmCrmMemberPointOverlayPort {

	/**
	 * 与外部积分查询对齐：至少包含 integral、frozenIntegral（long）。若仅返回当前积分：frozenIntegral 固定为 0L。
	 */
	Map<String, Object> fetchPointOverlay(long companyId, String mobilePlain);
}
