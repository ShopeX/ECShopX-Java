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

package cn.shopex.ecshopx.orders.service.setting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OrderValiditySettingDefaults {

	public static final String REDIS_KEY = "order_validity_setting";

	public static final Map<String, Object> DEFAULTS;

	static {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("order_finish_time", 7);
		m.put("order_cancel_time", 15);
		m.put("notpay_order_wxapp_notice", 0);
		m.put("latest_aftersale_time", 0);
		m.put("aftersale_close_time", 7);
		m.put("auto_refuse_time", 0);
		m.put("auto_aftersales", Boolean.FALSE);
		m.put("offline_aftersales", Boolean.FALSE);
		m.put("is_refund_freight", 0);
		DEFAULTS = Collections.unmodifiableMap(m);
	}

	private OrderValiditySettingDefaults() {}
}
