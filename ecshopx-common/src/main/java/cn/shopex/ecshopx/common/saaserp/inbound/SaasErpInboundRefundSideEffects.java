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

package cn.shopex.ecshopx.common.saaserp.inbound;

import java.util.function.Supplier;

/**
 * Marks {@code ome_create_reimburse} mall state updates so nested order-cancel logic does not
 * also emit {@code store.trade.update}; the inbound act performs that call once.
 */
public final class SaasErpInboundRefundSideEffects {

	private static final ThreadLocal<Boolean> SUPPRESS_OUTBOUND_TRADE_UPDATE = new ThreadLocal<>();

	private SaasErpInboundRefundSideEffects() {}

	public static boolean suppressOutboundTradeUpdate() {
		return Boolean.TRUE.equals(SUPPRESS_OUTBOUND_TRADE_UPDATE.get());
	}

	public static <T> T runSuppressingOutboundTradeUpdate(Supplier<T> work) {
		SUPPRESS_OUTBOUND_TRADE_UPDATE.set(Boolean.TRUE);
		try {
			return work.get();
		} finally {
			SUPPRESS_OUTBOUND_TRADE_UPDATE.remove();
		}
	}
}
