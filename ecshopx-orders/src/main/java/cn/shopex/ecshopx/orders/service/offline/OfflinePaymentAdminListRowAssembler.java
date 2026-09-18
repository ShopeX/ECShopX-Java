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

package cn.shopex.ecshopx.orders.service.offline;

import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OfflinePaymentAdminListRowAssembler {

	private final ObjectMapper objectMapper;

	public OfflinePaymentAdminListRowAssembler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> toListRowMap(OfflinePayment row) {
		ObjectMapper rowMapper =
				objectMapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		Map<String, Object> snakeRow =
				rowMapper.convertValue(row, new TypeReference<Map<String, Object>>() {});
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(snakeRow);

		String voucherRaw = row.getVoucherPic();
		List<Object> voucherList;
		if (voucherRaw == null || voucherRaw.isEmpty()) {
			voucherList = Collections.emptyList();
		} else {
			try {
				voucherList = objectMapper.readValue(voucherRaw, new TypeReference<List<Object>>() {});
			} catch (Exception e) {
				voucherList = Collections.emptyList();
			}
		}
		out.put("voucher_pic", voucherList);
		return out;
	}
}
