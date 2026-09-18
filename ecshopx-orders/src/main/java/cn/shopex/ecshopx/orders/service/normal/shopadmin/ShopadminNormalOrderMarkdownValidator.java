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

package cn.shopex.ecshopx.orders.service.normal.shopadmin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopadminNormalOrderMarkdownValidator {

	public void validate(Object markdown) {
		if (!(markdown instanceof Map<?, ?> raw)) {
			throw new ResourceException("改价参数格式错误");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> m = (Map<String, Object>) raw;
		Object downTypeObj = m.get("down_type");
		String downType = downTypeObj == null ? "" : downTypeObj.toString().trim();
		if (!"total".equals(downType) && !"items".equals(downType)) {
			throw new ResourceException("确认整单还是按件改价");
		}
		if ("total".equals(downType)) {
			if (!m.containsKey("total_fee") || m.get("total_fee") == null
					|| !StringUtils.hasText(String.valueOf(m.get("total_fee")).trim())) {
				throw new ResourceException("整单改价金额必填");
			}
		}
		if ("items".equals(downType)) {
			Object itemsRaw = m.get("items");
			if (!(itemsRaw instanceof List<?> itemsList) || itemsList.isEmpty()) {
				throw new ResourceException("按件改价商品ID必填");
			}
			List<Map<String, Object>> rows = new ArrayList<>();
			for (Object o : itemsList) {
				if (o instanceof Map<?, ?> row) {
					@SuppressWarnings("unchecked")
					Map<String, Object> rm = (Map<String, Object>) row;
					rows.add(rm);
				}
			}
			if (rows.isEmpty()) {
				throw new ResourceException("按件改价商品ID必填");
			}
			for (Map<String, Object> row : rows) {
				if (!row.containsKey("item_id") || row.get("item_id") == null
						|| !StringUtils.hasText(String.valueOf(row.get("item_id")).trim())) {
					throw new ResourceException("按件改价商品ID必填");
				}
				boolean hasTotalFee = row.containsKey("total_fee") && row.get("total_fee") != null
						&& StringUtils.hasText(String.valueOf(row.get("total_fee")).trim());
				boolean hasDiscount = row.containsKey("discount") && row.get("discount") != null
						&& StringUtils.hasText(String.valueOf(row.get("discount")).trim());
				if (!hasTotalFee && !hasDiscount) {
					throw new ResourceException("按件改价商品总价和折扣必须设置一个");
				}
			}
		}
	}
}
