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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** 对齐 PHP {@code ShuyunOpenPlatformOrderSyncDispatchCacheKeys}（DELTA-003 复用）。 */
public final class OrderSyncDispatchCacheKeys {

	public static final int TRADE_SYNC_DEDUPE_TTL_SEC = 600;
	public static final int TRADE_SYNC_DEDUPE_TTL_SEC_PER_TRIGGER = 30;
	public static final int REFUND_SYNC_DEDUPE_TTL_SEC_PER_LANE = 30;

	public static final String LOG_DISPATCH_DEDUPED = "shuyun_open_platform_dispatch_deduped";

	private OrderSyncDispatchCacheKeys() {}

	public static String tradeSyncDedupeKey(long companyId, String orderId) {
		return "shuyun_open_platform:dispatch_dedupe:trade_sync:" + companyId + ":" + sha1(orderId);
	}

	public static String tradeSyncDedupeKeyByTrigger(long companyId, String orderId, String trigger) {
		return "shuyun_open_platform:dispatch_dedupe:trade_sync:"
				+ companyId
				+ ":"
				+ sha1(orderId)
				+ ":"
				+ trigger;
	}

	public static String refundSyncDedupeKey(long companyId, String refundBn, String lane) {
		return "shuyun_open_platform:dispatch_dedupe:refund_sync:"
				+ companyId
				+ ":"
				+ sha1(refundBn)
				+ ":"
				+ lane;
	}

	public static String shopSyncMergeKey(long companyId, long distributorId) {
		return phpMergeCacheKey("shop_sync:" + companyId + ":" + distributorId);
	}

	/** 对齐 PHP {@code categorySyncMergeKey} → Redis {@code shuyun_open_platform:merge:} + sha256。 */
	public static String categorySyncMergeKey(long companyId, long categoryId) {
		return phpMergeCacheKey("category_sync:" + companyId + ":" + categoryId);
	}

	/** 对齐 PHP {@code productSyncMergeKey}。 */
	public static String productSyncMergeKey(long companyId, long distributorId, long defaultItemId) {
		return phpMergeCacheKey("product_sync:" + companyId + ":" + distributorId + ":" + defaultItemId);
	}

	static String phpMergeCacheKey(String logicalMergeKey) {
		return "shuyun_open_platform:merge:" + sha256(logicalMergeKey);
	}

	static String sha256(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] dig = md.digest((raw == null ? "" : raw).getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	static String sha1(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest((raw == null ? "" : raw).getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}
}
