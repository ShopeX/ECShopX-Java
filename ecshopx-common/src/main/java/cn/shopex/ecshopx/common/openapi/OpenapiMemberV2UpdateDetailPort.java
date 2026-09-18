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

public interface OpenapiMemberV2UpdateDetailPort {

	/**
	 * V2 ecx.member_info.update：按 mobile 定位会员并全量更新 members + members_info。
	 *
	 * @param companyId   鉴权后的企业 ID
	 * @param mergedRaw   mergeAll 结果（含 mobile，供 checkMobile + Action validation）
	 * @param requestData buildRequestData 结果（全量默认值，供 Service 更新）
	 */
	void updateMemberInfo(long companyId, Map<String, Object> mergedRaw, Map<String, Object> requestData);
}
