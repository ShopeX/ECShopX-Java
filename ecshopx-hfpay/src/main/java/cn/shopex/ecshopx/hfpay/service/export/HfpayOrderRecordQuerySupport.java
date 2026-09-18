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

package cn.shopex.ecshopx.hfpay.service.export;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/** Shared query normalization and date parsing for order record list/export. */
public final class HfpayOrderRecordQuerySupport {

	private HfpayOrderRecordQuerySupport() {}

	public static long[] validateAndParseEpoch(String startDate, String endDate, ZoneId zone) {
		String sd = startDate == null ? "" : startDate.trim();
		String ed = endDate == null ? "" : endDate.trim();
		if (sd.isEmpty() || ed.isEmpty()) {
			throw new ResourceException("请选择对应的日期范围");
		}
		try {
			LocalDate.parse(sd);
			LocalDate.parse(ed);
		} catch (DateTimeParseException e) {
			throw new ResourceException("日期格式有误");
		}
		long startEpoch = LocalDate.parse(sd).atStartOfDay(zone).toEpochSecond();
		long endEpoch = LocalDate.parse(ed).atTime(23, 59, 59).atZone(zone).toEpochSecond();
		return new long[] {startEpoch, endEpoch};
	}

	public static String normalizeOrderId(String orderId) {
		String orderIdTrim = orderId == null ? "" : orderId.trim();
		if (orderIdTrim.isEmpty() || "0".equals(orderIdTrim)) {
			return null;
		}
		return orderIdTrim;
	}

	public static String normalizeAppPayType(String appPayType) {
		String appPay = appPayType == null ? "" : appPayType.trim();
		return appPay.isEmpty() ? null : appPay;
	}

	public static Integer parseProfitsharingStatus(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String t = raw.trim();
		if ("0".equals(t)) {
			return null;
		}
		try {
			int n = Integer.parseInt(t);
			return n != 0 ? Integer.valueOf(n) : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
