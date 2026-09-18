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

package cn.shopex.ecshopx.common.cron.mock;

import cn.shopex.ecshopx.common.dispatch.AddDistributorItemsJobDispatchPublisher;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** test-cron profile: avoids enqueueing add-distributor-items dispatch jobs. */
@Slf4j
public class NoopAddDistributorItemsJobDispatchPublisher implements AddDistributorItemsJobDispatchPublisher {

	@Override
	public void enqueueFirstPage(
			long companyId,
			long distributorId,
			List<Long> itemIdsFilterOrNullForAll,
			boolean isCanSale,
			int pageSize) {
		log.info(
				"[cron-mock][add-distributor-items-job] enqueueFirstPage companyId={} distributorId={} pageSize={}",
				companyId,
				distributorId,
				pageSize);
	}

	@Override
	public void enqueuePage(
			long companyId,
			long distributorId,
			List<Long> itemIdsFilterOrNullForAll,
			boolean isCanSale,
			int page,
			int pageSize) {
		log.info(
				"[cron-mock][add-distributor-items-job] enqueuePage companyId={} distributorId={} page={} pageSize={}",
				companyId,
				distributorId,
				page,
				pageSize);
	}
}
