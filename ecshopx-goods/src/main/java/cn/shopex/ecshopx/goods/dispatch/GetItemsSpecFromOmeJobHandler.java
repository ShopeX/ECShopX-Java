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
import cn.shopex.ecshopx.goods.service.ome.OmeItemSpecFromOmePagedSyncRunner;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * {@link DispatchHandler} for {@code GoodsBundleDispatchJobNames#GET_ITEMS_SPEC_FROM_OME}: dequeue path on the
 * dispatch bus, delegating the payload to {@link OmeItemSpecFromOmePagedSyncRunner#consumeQueuedPage}.
 * <p>
 * Paged follow-up jobs are enqueued from the runner via {@code GetItemsSpecFromOmeJobDispatchPublisher} and
 * {@code DispatchFacade#dispatchJob}; inventory anchor {@code entry-02-jc-getitemsspecfromome-handle}.
 */
@Component
public class GetItemsSpecFromOmeJobHandler implements DispatchHandler {

	private final OmeItemSpecFromOmePagedSyncRunner pagedSyncRunner;

	public GetItemsSpecFromOmeJobHandler(OmeItemSpecFromOmePagedSyncRunner pagedSyncRunner) {
		this.pagedSyncRunner = pagedSyncRunner;
	}

	/**
	 * Runs one queued page for the item-spec list sync window. See {@link OmeItemSpecFromOmePagedSyncRunner} and
	 * {@code entry-02-jc-getitemsspecfromome-handle}.
	 */
	@Override
	public void handle(Map<String, Object> payload) {
		pagedSyncRunner.consumeQueuedPage(payload);
	}
}
