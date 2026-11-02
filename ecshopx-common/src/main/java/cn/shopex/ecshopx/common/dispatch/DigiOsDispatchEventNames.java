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

package cn.shopex.ecshopx.common.dispatch;

public final class DigiOsDispatchEventNames {

	/** 售后退货入库 → DigiOS 门店库存调整单（对齐 PHP Job）。 */
	public static final String EVENT_AFTERSALES_RETURN_ADJUSTMENT =
			"event:digios:aftersales_return_adjustment";

	public static final String LISTENER_DIGIOS_STORE_ADJUSTMENT_ON_AFTERSALES_RETURN =
			"listener:digios.store_adjustment_on_aftersales_return";

	private DigiOsDispatchEventNames() {}
}
