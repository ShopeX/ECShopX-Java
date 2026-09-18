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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RefundSyncDispatchService {

	private static final Logger log = LoggerFactory.getLogger(RefundSyncDispatchService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final OpenPlatformDispatchDedupeService dedupeService;
	private final RefundSyncService refundSyncService;

	public RefundSyncDispatchService(
			OpenPlatformConfigService openPlatformConfigService,
			OpenPlatformDispatchDedupeService dedupeService,
			RefundSyncService refundSyncService) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.dedupeService = dedupeService;
		this.refundSyncService = refundSyncService;
	}

	public void dispatchFinish(long companyId, String refundBn) {
		if (companyId < 1 || !StringUtils.hasText(refundBn)) {
			return;
		}
		CompanyShuyunOpenPlatformConfig row = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(row)) {
			return;
		}
		String key = OrderSyncDispatchCacheKeys.refundSyncDedupeKey(companyId, refundBn, "finish");
		if (!dedupeService.tryAcquire(key, OrderSyncDispatchCacheKeys.REFUND_SYNC_DEDUPE_TTL_SEC_PER_LANE)) {
			return;
		}
		log.info("Shuyun refund_sync_job_dispatched companyId={} refundBn={} lane=finish", companyId, refundBn);
		refundSyncService.syncRefund(companyId, refundBn);
	}
}
