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

package cn.shopex.ecshopx.common.port.systemlink;

import java.util.List;
import java.util.Map;

/**
 * Builds {@code sales.Trade.orderAdd} body triples {@code [shop_no, raw_trade_list, trade_order_list]} from an ERP
 * cancel payload map (snake_case keys) plus persisted order/trade data.
 */
public interface WdtErpTradeCancelOrderAddAssemblePort {

	/**
	 * @param wdtShopNo resolved shop number (company default or distributor {@code wdt_shop_no})
	 * @return each element is one invocation body: three entries matching WDT OpenAPI JSON array shape
	 */
	List<List<Object>> assembleOrderAddBodies(
			long companyId, Map<String, Object> erpCancelPayloadSnakeCase, String wdtShopNo);
}
