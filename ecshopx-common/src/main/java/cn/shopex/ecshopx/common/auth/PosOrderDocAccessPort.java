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

package cn.shopex.ecshopx.common.auth;

import jakarta.servlet.http.HttpServletRequest;

/**
 * G-P2/P3/B4：POS 会话下对订单写操作做本店 + 经手校验。非 POS 会话直接放行。
 * 由 orders 模块实现；aftersales 等通过 Port 调用。
 */
public interface PosOrderDocAccessPort {

	void assertWriteAccess(HttpServletRequest request, long companyId, long orderId);
}
