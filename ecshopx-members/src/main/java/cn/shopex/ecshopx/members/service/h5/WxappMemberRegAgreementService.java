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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappMemberRegAgreementService {

	private static final String TYPE_MEMBER_REGISTER = "member_register";

	private final ShopProtocolSetService shopProtocolSetService;

	public WxappMemberRegAgreementService(ShopProtocolSetService shopProtocolSetService) {
		this.shopProtocolSetService = shopProtocolSetService;
	}

	public Map<String, Object> getRegAgreementSetting(long companyId, String countryCode) {
		Map<String, Object> typeBlock =
				shopProtocolSetService.get(companyId, TYPE_MEMBER_REGISTER, countryCode);
		Object innerObj = typeBlock.get(TYPE_MEMBER_REGISTER);
		Map<?, ?> inner =
				innerObj instanceof Map<?, ?> m ? m : Collections.emptyMap();

		String typeStr = inner.get("type") == null ? "" : String.valueOf(inner.get("type"));
		String titleStr = inner.get("title") == null ? "" : String.valueOf(inner.get("title"));
		String contentStr = inner.get("content") == null ? "" : String.valueOf(inner.get("content"));

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("type", typeStr);
		out.put("title", titleStr);
		out.put("content", contentStr);
		return out;
	}
}
