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

package cn.shopex.ecshopx.common.port.theme;

import java.util.Map;

/**
 * Binds default storefront page templates when a new shop is created under a company.
 */
public interface NewDistributorPagesTemplatePort {

	/**
	 * Applies headquarters template defaults to the new distributor.
	 *
	 * @param companyId company scope
	 * @param distributorId new shop id
	 * @param distributorRowSnapshot persisted API row snapshot; may include {@code regionauth_id} and optional
	 *     locale hints
	 */
	void bindDefaultTemplates(long companyId, long distributorId, Map<String, Object> distributorRowSnapshot);
}
