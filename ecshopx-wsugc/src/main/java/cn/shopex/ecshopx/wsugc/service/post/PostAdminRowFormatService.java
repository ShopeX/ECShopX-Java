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

package cn.shopex.ecshopx.wsugc.service.post;

import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.goods.service.ugc.UgcPostItemListForDetailService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Badge;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.BadgeMapper;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import cn.shopex.ecshopx.wsugc.service.badge.BadgeDetailService;
import cn.shopex.ecshopx.wsugc.service.setting.UgcOfficialUserInfoReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PostAdminRowFormatService {

	private static final Logger log = LoggerFactory.getLogger(PostAdminRowFormatService.class);
	private static final DateTimeFormatter CREATED_TEXT_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	public enum Mode {
		POST_LIST,
		POST_DETAIL
	}

	private final MemberAccountService memberAccountService;
	private final UgcOfficialUserInfoReadService officialUserInfoReadService;
	private final TopicMapper topicMapper;
	private final BadgeMapper badgeMapper;
	private final BadgeDetailService badgeDetailService;
	private final UgcPostItemListForDetailService ugcPostItemListForDetailService;

	public PostAdminRowFormatService(
			MemberAccountService memberAccountService,
			UgcOfficialUserInfoReadService officialUserInfoReadService,
			TopicMapper topicMapper,
			BadgeMapper badgeMapper,
			BadgeDetailService badgeDetailService,
			UgcPostItemListForDetailService ugcPostItemListForDetailService) {
		this.memberAccountService = memberAccountService;
		this.officialUserInfoReadService = officialUserInfoReadService;
		this.topicMapper = topicMapper;
		this.badgeMapper = badgeMapper;
		this.badgeDetailService = badgeDetailService;
		this.ugcPostItemListForDetailService = ugcPostItemListForDetailService;
	}

	public void formatAdminRow(
			LinkedHashMap<String, Object> m,
			Post entityRow,
			long companyId,
			String requestLangTag,
			Mode mode) {
		Object created = m.get("created");
		if (created != null) {
			long sec = toEpochSeconds(created);
			if (sec > 0L) {
				m.put("created_text", CREATED_TEXT_FMT.format(Instant.ofEpochSecond(sec)));
			}
		}
		int status = parseIntLoose(m.get("status"));
		m.put("status", status);
		m.put("status_text", postStatusText(status));

		Long uid = longOrNull(m.get("user_id"));
		long postCompanyId = readLong(m.get("company_id"), companyId);
		if (uid != null && uid != 0L) {
			Map<String, Object> filter = Map.of("user_id", uid, "company_id", postCompanyId);
			Map<String, Object> wechat = memberAccountService.getWechatUserInfo(filter);
			Map<String, Object> member = memberAccountService.getMemberInfo(uid, companyId);
			LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
			if (member != null && !member.isEmpty()) {
				merged.putAll(member);
			}
			if (wechat != null) {
				merged.putAll(wechat);
			}
			int source = parseIntLoose(m.get("source"));
			List<String> allowKeys =
					source == 2
							? List.of("username", "avatar", "headimgurl", "nickname", "user_id")
							: List.of("username", "avatar", "headimgurl", "nickname", "user_id", "mobile");
			Map<String, Object> userInfoOut = new TreeMap<>();
			for (String k : allowKeys) {
				if (merged.containsKey(k)) {
					userInfoOut.put(k, merged.get(k));
				}
			}
			m.put("userInfo", new LinkedHashMap<>(userInfoOut));
		} else {
			Map<String, String> official = officialUserInfoReadService.loadOfficialDisplay(companyId);
			LinkedHashMap<String, Object> ui = new LinkedHashMap<>();
			ui.put("nickname", official.getOrDefault("official.nickname", ""));
			ui.put("headimgurl", official.getOrDefault("official.headerimgurl", ""));
			m.put("userInfo", ui);
		}

		if (ValuePresence.hasEffectiveValue(m.get("topics"))) {
			String topicsOrigin = m.get("topics").toString();
			m.put("topics_origin", topicsOrigin);
			List<String> rawIds = splitCsvIds(topicsOrigin);
			if (!rawIds.isEmpty()) {
				List<Long> ids = new ArrayList<>();
				for (String seg : rawIds) {
					try {
						ids.add(Long.parseLong(seg.trim()));
					} catch (NumberFormatException ignored) {
					}
				}
				if (!ids.isEmpty()) {
					List<Topic> topicRows =
							topicMapper.selectList(
									new LambdaQueryWrapper<Topic>()
											.eq(Topic::getCompanyId, companyId)
											.in(Topic::getTopicId, ids));
					Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
					for (Topic t : topicRows) {
						if (t.getTopicId() != null) {
							byId.put(t.getTopicId(), topicToDetailRow(t));
						}
					}
					List<Map<String, Object>> ordered = new ArrayList<>();
					for (Long id : ids) {
						Map<String, Object> tr = byId.get(id);
						if (tr != null) {
							ordered.add(tr);
						}
					}
					m.put("topics", ordered);
				} else {
					m.put("topics", Collections.emptyList());
				}
			} else {
				m.put("topics", Collections.emptyList());
			}
		}

		if (mode != Mode.POST_LIST && ValuePresence.hasEffectiveValue(m.get("goods"))) {
			String goodsStr = m.get("goods").toString();
			List<String> rawIds = splitCsvIds(goodsStr);
			List<Long> ids = new ArrayList<>();
			for (String seg : rawIds) {
				try {
					ids.add(Long.parseLong(seg.trim()));
				} catch (NumberFormatException ignored) {
				}
			}
			if (!ids.isEmpty()) {
				Long uidForMember = null;
				if (uid != null && uid > 0L) {
					uidForMember = uid;
				}
				m.put(
						"goods",
						ugcPostItemListForDetailService.listForPostDetail(companyId, ids, uidForMember));
			} else {
				m.put("goods", Collections.emptyList());
			}
		}

		if (ValuePresence.hasEffectiveValue(m.get("badges"))) {
			String badgesStr = m.get("badges").toString();
			m.put("badges_origin", badgesStr);
			List<Long> ids = new ArrayList<>();
			for (String seg : badgesStr.split(",")) {
				if (!StringUtils.hasText(seg)) {
					continue;
				}
				try {
					ids.add(Long.parseLong(seg.trim()));
				} catch (NumberFormatException ignored) {
				}
			}
			if (log.isDebugEnabled()) {
				log.debug("post admin badges companyId={} badgeIdCount={}", companyId, ids.size());
			}
			if (!ids.isEmpty()) {
				List<Badge> badgeRows =
						badgeMapper.selectList(
								new LambdaQueryWrapper<Badge>()
										.eq(Badge::getCompanyId, companyId)
										.in(Badge::getBadgeId, ids)
										.eq(Badge::getStatus, 1));
				Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
				for (Badge b : badgeRows) {
					if (b.getBadgeId() != null) {
						LinkedHashMap<String, Object> br = BadgeDetailService.badgeToListRowMap(b);
						badgeDetailService.applyOutsideLangAndFormatForListRow(br, requestLangTag);
						byId.put(b.getBadgeId(), trimBadgeForAdminDetailRow(br));
					}
				}
				List<Map<String, Object>> ordered = new ArrayList<>();
				for (Long id : ids) {
					Map<String, Object> br = byId.get(id);
					if (br != null) {
						ordered.add(br);
					}
				}
				m.put("badges", ordered);
			} else {
				m.put("badges", Collections.emptyList());
			}
		}

		if (m.containsKey("mobile") && m.get("mobile") != null) {
			m.remove("mobile");
		}
		if (m.containsKey("ip") && m.get("ip") != null) {
			m.remove("ip");
		}
		m.put("like_status", 0);
		m.put("favorite_status", 0);
		m.put("follow_status", 0);

		if (mode == Mode.POST_LIST) {
			m.put("user_id_auth", 0);
		} else {
			m.put("user_id_auth", "");
		}
		applyScalarParity(m, entityRow);
	}

	private static void applyScalarParity(LinkedHashMap<String, Object> m, Post row) {
		Long ai = row.getAiVerifyTime();
		if (ai == null) {
			m.put("ai_verify_time", null);
		} else if (ai.longValue() == 0L) {
			m.put("ai_verify_time", "0");
		} else {
			m.put("ai_verify_time", String.valueOf(ai));
		}
		Long mv = row.getManualVerifyTime();
		if (mv == null) {
			m.put("manual_verify_time", null);
		} else if (mv.longValue() == 0L) {
			m.put("manual_verify_time", "0");
		} else {
			m.put("manual_verify_time", String.valueOf(mv));
		}

		if (row.getShareNums() == null) {
			m.put("share_nums", null);
		} else {
			m.put("share_nums", row.getShareNums());
		}
		String images = row.getImages();
		if (!StringUtils.hasText(images)) {
			m.put("images", null);
		}
		String imageTag = row.getImageTag();
		if (!StringUtils.hasText(imageTag)) {
			m.put("image_tag", null);
		}

		if (!stringishHasText(row.getManualRefuseReason())
				&& !stringishHasText(m.get("manual_refuse_reason"))) {
			m.remove("manual_refuse_reason");
			m.remove("manual_refuse_reason_lang");
		}
		if (!stringishHasText(row.getAiRefuseReason())
				&& !stringishHasText(m.get("ai_refuse_reason"))) {
			m.remove("ai_refuse_reason");
			m.remove("ai_refuse_reason_lang");
		}
	}

	private static boolean stringishHasText(Object v) {
		if (v == null) {
			return false;
		}
		return StringUtils.hasText(v.toString());
	}

	/**
	 * Admin post row badge shape: nested keys aligned with list-row formatter output (no company_id /
	 * is_top / p_order / source / updated / user_id on each badge row).
	 */
	private static LinkedHashMap<String, Object> trimBadgeForAdminDetailRow(Map<String, Object> src) {
		String[] keys = {
			"badge_id",
			"badge_memo",
			"badge_memo_lang",
			"badge_name",
			"badge_name_lang",
			"created",
			"created_text",
			"status",
			"status_text"
		};
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (String k : keys) {
			if (src.containsKey(k)) {
				out.put(k, src.get(k));
			}
		}
		return out;
	}

	private static Map<String, Object> topicToDetailRow(Topic t) {
		LinkedHashMap<String, Object> r = new LinkedHashMap<>();
		r.put("topic_id", t.getTopicId());
		r.put("topic_name", t.getTopicName());
		r.put("status", t.getStatus());
		r.put("created", t.getCreated());
		return r;
	}

	private static List<String> splitCsvIds(String csv) {
		List<String> out = new ArrayList<>();
		if (!StringUtils.hasText(csv)) {
			return out;
		}
		for (String seg : csv.split(",")) {
			if (StringUtils.hasText(seg)) {
				out.add(seg.trim());
			}
		}
		return out;
	}

	private static String postStatusText(int status) {
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

	private static long toEpochSeconds(Object created) {
		if (created instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(created.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long longOrNull(Object o) {
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

	private static long readLong(Object v, long defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
