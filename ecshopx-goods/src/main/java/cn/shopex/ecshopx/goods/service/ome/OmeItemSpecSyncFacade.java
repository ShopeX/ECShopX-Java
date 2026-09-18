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

package cn.shopex.ecshopx.goods.service.ome;

import cn.shopex.ecshopx.goods.dispatch.GetItemsSpecFromOmeJobDispatchPublisher;
import org.springframework.stereotype.Service;

/**
 * Admin entry point for enqueueing the first-page item-spec sync job for a company.
 * Delegates to the dispatch publisher with {@code page = 1} and an end-modify
 * timestamp in Unix seconds derived from the current wall clock.
 */
@Service
public class OmeItemSpecSyncFacade {

	private final GetItemsSpecFromOmeJobDispatchPublisher getItemsSpecFromOmeJobDispatchPublisher;

	public OmeItemSpecSyncFacade(GetItemsSpecFromOmeJobDispatchPublisher getItemsSpecFromOmeJobDispatchPublisher) {
		this.getItemsSpecFromOmeJobDispatchPublisher = getItemsSpecFromOmeJobDispatchPublisher;
	}

	/**
	 * Enqueues the initial item-spec-from-OME job for the given tenant.
	 *
	 * @param companyId tenant identifier from the authenticated operator context
	 */
	public void enqueueInitialSync(long companyId) {
		long endUnix = System.currentTimeMillis() / 1000L;
		getItemsSpecFromOmeJobDispatchPublisher.enqueueGetItemsSpecFromOme(companyId, 1, endUnix);
	}
}
