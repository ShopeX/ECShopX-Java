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

package cn.shopex.ecshopx.goods.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.goods.service.ome.OmeItemsFromOmePagedSyncRunner;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Bus-registered handler for the OME goods list sync job. Each invocation processes one queued page via
 * {@link OmeItemsFromOmePagedSyncRunner#consumeQueuedPage}. When additional pages remain for the same sync window,
 * the runner enqueues the next page through {@link GetItemsFromOmeJobDispatchPublisher}
 * after the current page is persisted, so the job forms a chain of same-named async messages rather than blocking
 * inside one consume call.
 * Migration artifact slug reference for traceability: entry-02.
 */
@Component
public class GetItemsFromOmeJobHandler implements DispatchHandler {

	private final OmeItemsFromOmePagedSyncRunner pagedSyncRunner;

	public GetItemsFromOmeJobHandler(OmeItemsFromOmePagedSyncRunner pagedSyncRunner) {
		this.pagedSyncRunner = pagedSyncRunner;
	}

	/**
	 * Delegates to the paged sync runner; see runner JavaDoc for when a follow-up job is scheduled on the bus.
	 */
	@Override
	public void handle(Map<String, Object> payload) {
		pagedSyncRunner.consumeQueuedPage(payload);
	}
}
