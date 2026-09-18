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

package cn.shopex.ecshopx.orders.service.tradeexport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TradeExportAsyncExecutor {

	private static final Logger log = LoggerFactory.getLogger(TradeExportAsyncExecutor.class);

	private final TradeExportFileJobHandler tradeExportFileJobHandler;

	public TradeExportAsyncExecutor(TradeExportFileJobHandler tradeExportFileJobHandler) {
		this.tradeExportFileJobHandler = tradeExportFileJobHandler;
	}

	@Async("ordersOrderExportDataExecutor")
	public void executeAsync(TradeExportJobContext ctx) {
		try {
			tradeExportFileJobHandler.run(ctx);
		} catch (Exception e) {
			log.error("trade export async failed", e);
		}
	}
}
