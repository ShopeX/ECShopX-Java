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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** D4 入队闸门 + dedupe，再同步执行 trade.sync（无账号时 Gateway 失败仅打日志）。 */
@Service
public class TradeSyncDispatchService {

	private static final Logger log = LoggerFactory.getLogger(TradeSyncDispatchService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final OpenPlatformDispatchDedupeService dedupeService;
	private final TradeSyncService tradeSyncService;
	private final JdbcTemplate jdbcTemplate;

	public TradeSyncDispatchService(
			OpenPlatformConfigService openPlatformConfigService,
			OpenPlatformDispatchDedupeService dedupeService,
			TradeSyncService tradeSyncService,
			JdbcTemplate jdbcTemplate) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.dedupeService = dedupeService;
		this.tradeSyncService = tradeSyncService;
		this.jdbcTemplate = jdbcTemplate;
	}

	public void dispatchPaySuccess(long companyId, String orderId) {
		dispatch(companyId, orderId, "pay_success", false);
	}

	public void dispatchDelivery(long companyId, String orderId) {
		dispatch(companyId, orderId, "delivery", false);
	}

	public void dispatchConfirmReceipt(long companyId, String orderId) {
		dispatch(companyId, orderId, "confirm_receipt", false);
	}

	public void dispatchOrderCancel(long companyId, String orderId) {
		dispatch(companyId, orderId, "order_cancel", true);
	}

	public void dispatchRefundFinish(long companyId, String orderId) {
		dispatch(companyId, orderId, "refund_finish", true);
	}

	public void dispatch(long companyId, String orderId, String trigger, boolean requirePayed) {
		if (companyId < 1 || !StringUtils.hasText(orderId)) {
			return;
		}
		CompanyShuyunOpenPlatformConfig row = openPlatformConfigService.findByCompanyId(companyId);
		if (!InboundSignedCallbackPreparer.isEligible(row)) {
			log.info(
					"Shuyun trade_sync dispatch skipped: open platform not eligible. companyId={} orderId={}",
					companyId,
					orderId);
			return;
		}
		if (requirePayed && !isPayedOrder(companyId, orderId)) {
			log.info(
					"Shuyun trade_sync dispatch skipped: order is not PAYED. companyId={} orderId={} trigger={}",
					companyId,
					orderId,
					trigger);
			return;
		}
		String key =
				"pay_success".equals(trigger)
						? OrderSyncDispatchCacheKeys.tradeSyncDedupeKey(companyId, orderId)
						: OrderSyncDispatchCacheKeys.tradeSyncDedupeKeyByTrigger(companyId, orderId, trigger);
		int ttl =
				"pay_success".equals(trigger)
						? OrderSyncDispatchCacheKeys.TRADE_SYNC_DEDUPE_TTL_SEC
						: OrderSyncDispatchCacheKeys.TRADE_SYNC_DEDUPE_TTL_SEC_PER_TRIGGER;
		if (!dedupeService.tryAcquire(key, ttl)) {
			return;
		}
		log.info(
				"Shuyun trade_sync_job_dispatched companyId={} orderId={} trigger={}",
				companyId,
				orderId,
				trigger);
		tradeSyncService.syncOrder(companyId, orderId);
	}

	private boolean isPayedOrder(long companyId, String orderId) {
		String status =
				jdbcTemplate.query(
						"""
						SELECT pay_status FROM orders_normal_orders
						WHERE company_id=? AND order_id=? LIMIT 1
						""",
						rs -> rs.next() ? rs.getString(1) : null,
						companyId,
						orderId);
		return "PAYED".equals(status);
	}
}
