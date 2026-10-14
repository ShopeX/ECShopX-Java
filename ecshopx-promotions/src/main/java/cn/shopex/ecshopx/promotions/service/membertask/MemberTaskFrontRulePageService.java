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
import org.springframework.stereotype.Service;

/** C 端任务中心规则页（ECX-10434 · 店铺级文案待配置源接入前返回空）。 */
@Service
public class MemberTaskFrontRulePageService {

	public Map<String, Object> rulePage(long companyId) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("content", "");
		return data;
	}
}
