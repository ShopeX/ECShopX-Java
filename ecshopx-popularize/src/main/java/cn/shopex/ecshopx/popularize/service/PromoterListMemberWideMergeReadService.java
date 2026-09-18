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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.popularize.mapper.PromoterListMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Loads member rows for admin promoter list merge using the same SQL projection as the legacy list
 * ({@code members.*}, {@code members_info.username}, {@code members_info.sex}).
 */
@Service
public class PromoterListMemberWideMergeReadService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CREATED_DATE_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final PromoterListMapper promoterListMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public PromoterListMemberWideMergeReadService(
			PromoterListMapper promoterListMapper, SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.promoterListMapper = promoterListMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public Map<Long, Map<String, Object>> batchRowMapsByUserIds(long companyId, Collection<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Map.of();
		}
		List<Long> ids = userIds.stream().filter(Objects::nonNull).filter(id -> id > 0L).distinct().toList();
		if (ids.isEmpty()) {
			return Map.of();
		}
		List<Map<String, Object>> rawRows =
				promoterListMapper.selectMemberRowsForAdminPromoterListMerge(companyId, ids);
		if (rawRows == null || rawRows.isEmpty()) {
			return Map.of();
		}
		LinkedHashMap<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map<String, Object> raw : rawRows) {
			if (raw == null || raw.isEmpty()) {
				continue;
			}
			LinkedHashMap<String, Object> row =
					PromoterAdminListMapKeySnakeCaseUtil.mapKeysCamelToSnake(new LinkedHashMap<>(raw));
			long uid = longFrom(row.get("user_id"));
			if (uid <= 0L) {
				continue;
			}
			decryptMemberContactFieldsInPlace(row);
			appendCreatedPresentationFields(row);
			out.put(uid, row);
		}
		return out;
	}

	private void decryptMemberContactFieldsInPlace(Map<String, Object> row) {
		Object mob = row.get("mobile");
		if (mob != null) {
			String s = String.valueOf(mob);
			row.put("mobile", sensitiveFieldEncryptor.decrypt(s));
		}
		Object un = row.get("username");
		if (un != null) {
			String s = String.valueOf(un);
			row.put("username", sensitiveFieldEncryptor.decrypt(s));
		}
	}

	private static void appendCreatedPresentationFields(Map<String, Object> row) {
		long ts = longFrom(row.get("created"));
		Integer cy = intOrNull(row.get("created_year"));
		Integer cm = intOrNull(row.get("created_month"));
		Integer cd = intOrNull(row.get("created_day"));
		ZonedDateTime zdt = ts > 0L ? Instant.ofEpochSecond(ts).atZone(SHANGHAI) : null;
		boolean hasDbYmd =
				cy != null
						&& cy > 0
						&& cm != null
						&& cm > 0
						&& cd != null
						&& cd > 0;
		if (hasDbYmd) {
			row.put("created_year", String.valueOf(cy));
			row.put("created_month", String.valueOf(cm));
			row.put("created_day", String.valueOf(cd));
		} else if (zdt != null) {
			row.put("created_year", String.valueOf(zdt.getYear()));
			row.put("created_month", String.valueOf(zdt.getMonthValue()));
			row.put("created_day", String.valueOf(zdt.getDayOfMonth()));
		} else {
			row.put("created_year", "0");
			row.put("created_month", "0");
			row.put("created_day", "0");
		}
		if (zdt != null) {
			row.put("created_date", zdt.format(CREATED_DATE_FMT));
		} else {
			row.put("created_date", "");
		}
	}

	private static long longFrom(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Integer intOrNull(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
