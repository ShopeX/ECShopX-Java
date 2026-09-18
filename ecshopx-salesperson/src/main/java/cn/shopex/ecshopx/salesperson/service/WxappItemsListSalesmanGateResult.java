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

package cn.shopex.ecshopx.salesperson.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WxappItemsListSalesmanGateResult {

	private final boolean returnEmpty;
	private final Integer nosalestore;
	private final Map<String, Object> paramsPatch;

	public WxappItemsListSalesmanGateResult(boolean returnEmpty, Integer nosalestore, Map<String, Object> paramsPatch) {
		this.returnEmpty = returnEmpty;
		this.nosalestore = nosalestore;
		this.paramsPatch = paramsPatch != null ? paramsPatch : Collections.emptyMap();
	}

	public boolean isReturnEmpty() {
		return returnEmpty;
	}

	public Integer getNosalestore() {
		return nosalestore;
	}

	public Map<String, Object> getParamsPatch() {
		return paramsPatch;
	}

	public static WxappItemsListSalesmanGateResult proceed(Map<String, Object> patch) {
		return new WxappItemsListSalesmanGateResult(false, null, patch != null ? patch : new LinkedHashMap<>());
	}

	public static WxappItemsListSalesmanGateResult empty(int nosalestore) {
		return new WxappItemsListSalesmanGateResult(true, nosalestore, Map.of());
	}
}
