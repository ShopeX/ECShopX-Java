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

package cn.shopex.ecshopx.digios.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/** ECX operators / POS 账号 → DigiOS 外部业务账户字段映射。 */
public final class DigiOsAccountMapper {

	private DigiOsAccountMapper() {}

	/** staff Operator：login_name→acc，username→nickNm。 */
	public static Map<String, String> map(Map<String, Object> operator) {
		if (operator == null) {
			return null;
		}
		return build(
				trim(operator.get("login_name")),
				trim(operator.get("username")),
				trim(operator.get("mobile")),
				trim(operator.get("email")));
	}

	/** POS 账号：employee_no→acc，name→nickNm（G-D2）。 */
	public static Map<String, String> mapPos(Map<String, Object> posAccount) {
		if (posAccount == null) {
			return null;
		}
		return build(
				trim(posAccount.get("employee_no")),
				trim(posAccount.get("name")),
				trim(posAccount.get("mobile")),
				trim(posAccount.get("email")));
	}

	private static Map<String, String> build(String acc, String nickNm, String mobilePhone, String email) {
		if (!StringUtils.hasText(acc) || !StringUtils.hasText(mobilePhone)) {
			return null;
		}
		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("acc", acc);
		payload.put("nickNm", StringUtils.hasText(nickNm) ? nickNm : acc);
		payload.put("mobilePhone", mobilePhone);
		payload.put("email", email);
		return payload;
	}

	private static String trim(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}
}
