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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 合并规则：先 query + form（含 activated 注入），再 JSON body 逐 key 覆盖；
 * 最后若存在 activated 的 distributor_id，再写回覆盖 body（店铺端选中店铺优先）。
 */
@Service
public class DiscountCardCreateRequestMergeService {

	public Map<String, Object> merge(HttpServletRequest request, Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null && !body.isEmpty()) {
			for (Map.Entry<String, Object> ent : body.entrySet()) {
				if (ent.getKey() != null && ent.getValue() != null) {
					merged.put(ent.getKey(), ent.getValue());
				}
			}
		}
		Object activatedDistributorId = request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID);
		if (activatedDistributorId != null) {
			merged.put("distributor_id", activatedDistributorId);
		}
		return merged;
	}
}
