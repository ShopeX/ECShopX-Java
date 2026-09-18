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

package cn.shopex.ecshopx.popularize.port;

import java.util.Map;

/**
 * HFPAY-backed merchant withdrawal for promoter rebate cash-out; implemented in bootstrap to avoid module cycles.
 */
public interface PopularizeHfpayRebateMerchantWithdrawalPort {

	Map<String, Object> merchantPayment(Map<String, Object> paymentData);
}
