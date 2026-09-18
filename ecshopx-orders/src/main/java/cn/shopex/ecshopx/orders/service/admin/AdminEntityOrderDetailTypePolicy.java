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

package cn.shopex.ecshopx.orders.service.admin;

import org.springframework.stereotype.Service;

/**
 * Predicate for whether a normal-order detail pipeline applies to the effective order type string.
 */
@Service
public class AdminEntityOrderDetailTypePolicy {

	public boolean supportsNormalPipeline(String effective) {
		return supportsNormalPipelineStatic(effective);
	}

	public static boolean supportsNormalPipelineStatic(String effective) {
		if (effective == null || effective.isEmpty()) {
			return false;
		}
		if ("membercard".equals(effective) || "supplier_order".equals(effective)) {
			return false;
		}
		if ("normal".equals(effective)
				|| "normal_shopadmin".equals(effective)
				|| "service".equals(effective)
				|| effective.startsWith("service_")
				|| "bargain".equals(effective)
				|| "normal_bargain".equals(effective)) {
			return true;
		}
		return effective.startsWith("normal_");
	}
}
