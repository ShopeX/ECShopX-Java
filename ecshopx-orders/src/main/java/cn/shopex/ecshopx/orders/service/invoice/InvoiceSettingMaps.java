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

package cn.shopex.ecshopx.orders.service.invoice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将 {@link cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService#getInvoiceSetting} 的返回值
 * 规范为 {@code Map<String, Object>}, 与 {@code UserInvoiceCreateTxService} / 前端开票的 JSON 判空、键访问语义同维，作为本
 * 类唯一解析入口，供周期任务与开票据共用，避免两处分叉。
 */
public final class InvoiceSettingMaps {

	private InvoiceSettingMaps() {}

	/** 将已解析的 JSON/Map 规范为可按键访问的 {@code Map}。 */
	public static Map<String, Object> asSettingMap(Object raw) {
		if (raw instanceof Map<?, ?> m) {
			Map<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
			return out;
		}
		return new LinkedHashMap<>();
	}
}
