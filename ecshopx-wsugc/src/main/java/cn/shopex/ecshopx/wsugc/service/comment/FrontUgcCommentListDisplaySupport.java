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

package cn.shopex.ecshopx.wsugc.service.comment;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * H5 comment list row display: relative time, WeChat-only userInfo for main rows, reply nickname.
 */
public final class FrontUgcCommentListDisplaySupport {

	private static final DateTimeFormatter CREATED_TEXT_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private FrontUgcCommentListDisplaySupport() {}

	public static void enrichMainRow(Map<String, Object> rowMap, MemberAccountService memberAccountService) {
		Object created = rowMap.get("created");
		long sec = toEpochSeconds(created);
		if (sec > 0L) {
			rowMap.put("created_text", CREATED_TEXT_FMT.format(Instant.ofEpochSecond(sec)));
		}
		rowMap.put("created", formatRelativeTimeSeconds(sec));

		LinkedHashMap<String, Object> filterUser = new LinkedHashMap<>();
		filterUser.put("user_id", rowMap.get("user_id"));
		filterUser.put("company_id", rowMap.get("company_id"));
		Map<String, Object> wx = memberAccountService.getWechatUserInfo(filterUser);
		rowMap.put("nickname", textOrEmpty(wx.get("nickname")));
		rowMap.put("headimgurl", textOrEmpty(wx.get("headimgurl")));

		LinkedHashMap<String, Object> userInfo = new LinkedHashMap<>();
		Object nick = wx.get("nickname");
		String nickanme =
				nick != null && StringUtils.hasText(nick.toString()) ? nick.toString().trim() : "";
		userInfo.put("nickanme", nickanme);
		userInfo.put("headimgurl", textOrEmpty(wx.get("headimgurl")));
		rowMap.put("userInfo", userInfo);

		Object replyUid = rowMap.get("reply_user_id");
		Long replyLong = replyUid instanceof Number n ? n.longValue() : parseLongOrNull(replyUid);
		if (replyLong != null && replyLong > 0L) {
			LinkedHashMap<String, Object> filterReply = new LinkedHashMap<>();
			filterReply.put("user_id", replyLong);
			filterReply.put("company_id", rowMap.get("company_id"));
			Map<String, Object> replyWx = memberAccountService.getWechatUserInfo(filterReply);
			rowMap.put("reply_nickname", textOrEmpty(replyWx.get("nickname")));
		}

		Object st = rowMap.get("status");
		int status = st instanceof Number n ? n.intValue() : parseIntLoose(st);
		rowMap.put("status_text", statusText(status));
	}

	public static void enrichChildRow(Map<String, Object> rowMap, MemberAccountService memberAccountService) {
		Object created = rowMap.get("created");
		long sec = toEpochSeconds(created);
		rowMap.put("created", formatRelativeTimeSeconds(sec));

		LinkedHashMap<String, Object> filterUser = new LinkedHashMap<>();
		filterUser.put("user_id", rowMap.get("user_id"));
		filterUser.put("company_id", rowMap.get("company_id"));
		Map<String, Object> wx = memberAccountService.getWechatUserInfo(filterUser);
		rowMap.put("nickname", textOrEmpty(wx.get("nickname")));
		rowMap.put("headimgurl", textOrEmpty(wx.get("headimgurl")));

		Object replyUid = rowMap.get("reply_user_id");
		Long replyLong = replyUid instanceof Number n ? n.longValue() : parseLongOrNull(replyUid);
		if (replyLong != null && replyLong > 0L) {
			LinkedHashMap<String, Object> filterReply = new LinkedHashMap<>();
			filterReply.put("user_id", replyLong);
			filterReply.put("company_id", rowMap.get("company_id"));
			Map<String, Object> replyWx = memberAccountService.getWechatUserInfo(filterReply);
			rowMap.put("reply_nickname", textOrEmpty(replyWx.get("nickname")));
		}
	}

	/**
	 * Relative display string for a Unix timestamp in the JVM default time zone: {@code 刚刚}, {@code N分钟前},
	 * same calendar day as {@code HH:mm}, {@code 昨天}/{@code 前天} plus time, {@code N天前} within ten days,
	 * same calendar year as {@code MM-dd}, otherwise {@code yyyy-MM-dd}.
	 */
	public static String formatCommentStyleMoment(long epochSeconds) {
		if (epochSeconds <= 0L) {
			return "";
		}
		ZoneId zone = ZoneId.systemDefault();
		long current = Instant.now().getEpochSecond();
		long seconds = current - epochSeconds;
		long minutes = seconds / 60;

		ZonedDateTime commentZdt = Instant.ofEpochSecond(epochSeconds).atZone(zone);
		LocalDate commentDate = commentZdt.toLocalDate();
		LocalDate today = LocalDate.now(zone);

		long date = commentDate.atStartOfDay(zone).toEpochSecond();
		long dateToday = today.atStartOfDay(zone).toEpochSecond();
		long days = (dateToday - date) / 86400;

		long year = commentDate.withDayOfYear(1).atStartOfDay(zone).toEpochSecond();
		long yearToday = today.withDayOfYear(1).atStartOfDay(zone).toEpochSecond();

		DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm").withZone(zone);
		DateTimeFormatter md = DateTimeFormatter.ofPattern("MM-dd").withZone(zone);
		DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zone);
		Instant commentInstant = Instant.ofEpochSecond(epochSeconds);

		if (minutes == 0) {
			return "刚刚";
		}
		if (minutes <= 10) {
			return minutes + "分钟前";
		}
		if (date == dateToday) {
			return hm.format(commentInstant);
		}
		if (days == 1) {
			return "昨天" + hm.format(commentInstant);
		}
		if (days == 2) {
			return "前天" + hm.format(commentInstant);
		}
		if (days <= 10) {
			return days + "天前";
		}
		if (year == yearToday) {
			return md.format(commentInstant);
		}
		return ymd.format(commentInstant);
	}

	/**
	 * Formats {@code epochSeconds} in the JVM default time zone as {@code yyyy-MM-dd}.
	 * Returns an empty string when {@code epochSeconds} is zero or negative.
	 */
	public static String formatDateYmd(long epochSeconds) {
		if (epochSeconds <= 0L) {
			return "";
		}
		ZoneId zone = ZoneId.systemDefault();
		return DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zone).format(Instant.ofEpochSecond(epochSeconds));
	}

	/**
	 * Formats {@code epochSeconds} in the JVM default time zone as {@code MM-dd}.
	 * Returns an empty string when {@code epochSeconds} is zero or negative.
	 */
	public static String formatDateMd(long epochSeconds) {
		if (epochSeconds <= 0L) {
			return "";
		}
		ZoneId zone = ZoneId.systemDefault();
		return DateTimeFormatter.ofPattern("MM-dd").withZone(zone).format(Instant.ofEpochSecond(epochSeconds));
	}

	private static String formatRelativeTimeSeconds(long timeSec) {
		return formatCommentStyleMoment(timeSec);
	}

	private static String statusText(int status) {
		return switch (status) {
			case 0 -> "待审核";
			case 1 -> "审核通过";
			case 2 -> "机器拒绝";
			case 3 -> "待人工审核";
			case 4 -> "人工拒绝";
			default -> "";
		};
	}

	private static int parseIntLoose(Object st) {
		if (st == null) {
			return -1;
		}
		if (st instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(st.toString().trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static Long parseLongOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long toEpochSeconds(Object created) {
		if (created instanceof Number n) {
			return n.longValue();
		}
		if (created == null) {
			return 0L;
		}
		try {
			return Long.parseLong(created.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String textOrEmpty(Object o) {
		if (o == null) {
			return "";
		}
		return o.toString();
	}
}
