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

package cn.shopex.ecshopx.orders.service.espier;

import java.util.List;
import java.util.Map;

/**
 * Validates one split-shipment segment before creating a delivery. The returned list matches
 * {@code segment} size; a {@code null} element means that row passed.
 */
public interface NormalOrdersUploadSplitValidator {
	List<String> validate(long companyId, long supplierId, List<Map<String, Object>> segment);
}
