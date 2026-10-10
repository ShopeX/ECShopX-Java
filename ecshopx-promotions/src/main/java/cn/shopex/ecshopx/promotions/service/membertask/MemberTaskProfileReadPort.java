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

package cn.shopex.ecshopx.promotions.service.membertask;

import java.util.LinkedHashMap;
import java.util.Map;

public interface MemberTaskProfileReadPort {

	/** 开启中的会员个人信息字段（key_name → 字段配置，含 is_required / is_default）。 */
	LinkedHashMap<String, Map<String, Object>> loadOpenMemberInfoFields(long companyId);

	/** 会员资料快照（含 mobile、标准列、other_params.custom_data）。 */
	Map<String, Object> loadMemberInfo(long companyId, long userId);
}
