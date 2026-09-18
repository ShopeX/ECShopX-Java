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

package cn.shopex.ecshopx.adapay.service.callback;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AdapayCallbackEventTypeRegistry {

	public enum HandlerKind {
		PAYMENT,
		CORP_MEMBER,
		CORP_MEMBER_UPDATE,
		PAYMENT_REVERSE
	}

	private static final Map<String, HandlerKind> BY_TYPE;

	static {
		Map<String, HandlerKind> m = new HashMap<>();
		m.put("payment.succeeded", HandlerKind.PAYMENT);
		m.put("payment.failed", HandlerKind.PAYMENT);
		m.put("corp_member.succeeded", HandlerKind.CORP_MEMBER);
		m.put("corp_member.failed", HandlerKind.CORP_MEMBER);
		m.put("corp_member_update.succeeded", HandlerKind.CORP_MEMBER_UPDATE);
		m.put("corp_member_update.failed", HandlerKind.CORP_MEMBER_UPDATE);
		m.put("payment_reverse.succeeded", HandlerKind.PAYMENT_REVERSE);
		m.put("payment_reverse.failed", HandlerKind.PAYMENT_REVERSE);
		BY_TYPE = Collections.unmodifiableMap(m);
	}

	public HandlerKind resolve(String eventType) {
		return BY_TYPE.get(eventType);
	}
}
