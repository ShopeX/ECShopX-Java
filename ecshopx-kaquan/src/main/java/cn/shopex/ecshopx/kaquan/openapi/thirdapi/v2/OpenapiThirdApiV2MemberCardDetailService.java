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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardSetService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberCardDetailService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static final List<String> STRING_COLUMNS =
			List.of("brand_name", "logo_url", "title", "color", "background_pic_url");

	private final MemberCardSetService memberCardSetService;

	public OpenapiThirdApiV2MemberCardDetailService(MemberCardSetService memberCardSetService) {
		this.memberCardSetService = memberCardSetService;
	}

	public Map<String, Object> executeOpenapiDetail(long companyId) {
		Map<String, Object> raw = memberCardSetService.getMemberCard(companyId);

		Map<String, Object> result = new LinkedHashMap<>();
		for (String col : STRING_COLUMNS) {
			result.put(col, toOpenapiString(raw.get(col)));
		}
		result.put("created", formatUnixTimestamp(raw.get("created")));
		result.put("updated", formatUnixTimestamp(raw.get("updated")));
		return result;
	}

	/** PHP isset($result[$col]) ? (string)$val : "" — null 或键不存在均输出 ""。 */
	private static String toOpenapiString(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	/** PHP isset + Carbon::createFromTimestamp；缺失/null/非 Number → ""。 */
	private static String formatUnixTimestamp(Object value) {
		if (!(value instanceof Number number)) {
			return "";
		}
		long seconds = number.longValue();
		return DATETIME_FMT.format(Instant.ofEpochSecond(seconds));
	}
}
