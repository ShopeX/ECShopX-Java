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

package cn.shopex.ecshopx.common.port.pointsmall;

import java.util.Map;

public interface PointsmallFreightMoneyConvertPort {

	/**
	 * Converts freight money (fen) per company pointsmall base setting.
	 *
	 * @return map with {@code freight_type} and {@code money} (converted freight amount)
	 */
	Map<String, Object> moneyToPoint(long companyId, long moneyFen);
}
