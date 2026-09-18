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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.members.port.WxappMemberWechatUserRowPort;
import cn.shopex.ecshopx.common.port.reservation.ReservationUserWxappIdentityResolvePort;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReservationUserWxappIdentityResolvePortImpl implements ReservationUserWxappIdentityResolvePort {

	private final WxappMemberWechatUserRowPort wxappMemberWechatUserRowPort;

	public ReservationUserWxappIdentityResolvePortImpl(WxappMemberWechatUserRowPort wxappMemberWechatUserRowPort) {
		this.wxappMemberWechatUserRowPort = wxappMemberWechatUserRowPort;
	}

	@Override
	public void fillIfMissing(long companyId, long userId, Map<String, Object> mergedParams) {
		if (userId <= 0L) {
			return;
		}
		boolean needApp =
				!StringUtils.hasText(stringVal(mergedParams.get("wxapp_appid")))
						&& !StringUtils.hasText(stringVal(mergedParams.get("wxappAppid")));
		boolean needOpen =
				!StringUtils.hasText(stringVal(mergedParams.get("open_id")))
						&& !StringUtils.hasText(stringVal(mergedParams.get("openId")))
						&& !StringUtils.hasText(stringVal(mergedParams.get("openid")));
		if (!needApp && !needOpen) {
			return;
		}
		Map<String, Object> row = wxappMemberWechatUserRowPort.loadByUserId(companyId, userId);
		if (row == null || row.isEmpty()) {
			return;
		}
		if (needApp) {
			Object app = row.get("authorizer_appid");
			if (app != null && StringUtils.hasText(String.valueOf(app).trim())) {
				mergedParams.put("wxapp_appid", String.valueOf(app).trim());
			}
		}
		if (needOpen) {
			Object oid = row.get("open_id");
			if (oid != null && StringUtils.hasText(String.valueOf(oid).trim())) {
				mergedParams.put("open_id", String.valueOf(oid).trim());
			}
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
