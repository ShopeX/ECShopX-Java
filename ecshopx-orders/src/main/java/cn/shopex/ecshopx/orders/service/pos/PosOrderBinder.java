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

package cn.shopex.ecshopx.orders.service.pos;

import cn.shopex.ecshopx.common.auth.PosJwtRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 从当前请求 POS JWT 解析经手账号 id；无 POS 会话则 0（operator 代客）。 */
public final class PosOrderBinder {

	private PosOrderBinder() {}

	public static long currentPosStoreAccountId() {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		if (!(attrs instanceof ServletRequestAttributes sra)) {
			return 0L;
		}
		HttpServletRequest request = sra.getRequest();
		if (request == null) {
			return 0L;
		}
		Object raw = request.getAttribute(PosJwtRequestAttributes.POS_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> claims)) {
			return 0L;
		}
		Object id = claims.get("pos_store_account_id");
		if (id == null) {
			id = claims.get("id");
		}
		if (id instanceof Number n) {
			return n.longValue();
		}
		if (id == null) {
			return 0L;
		}
		try {
			return Long.parseLong(id.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
