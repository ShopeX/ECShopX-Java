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
import cn.shopex.ecshopx.wsugc.domain.PostFavorite;
import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.BadgeMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostFavoriteMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import cn.shopex.ecshopx.wsugc.service.badge.BadgeDetailService;
import cn.shopex.ecshopx.wsugc.service.follower.FrontUgcFollowerStatService;
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

/** Builds embedded {@code postInfo} for H5 message detail (full post row + topics/goods/badges/userInfo). */
@Service
public class FrontUgcPostDetailEmbedService {

	private static final Logger log = LoggerFactory.getLogger(FrontUgcPostDetailEmbedService.class);
	private static final DateTimeFormatter CREATED_TEXT_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final PostMapper postMapper;
	private final PostOutsideLangReadService postOutsideLangReadService;
	private final MemberAccountService memberAccountService;
	private final UgcOfficialUserInfoReadService officialUserInfoReadService;
	private final TopicMapper topicMapper;
	private final BadgeMapper badgeMapper;
	private final BadgeDetailService badgeDetailService;
	private final UgcPostItemListForDetailService ugcPostItemListForDetailService;
	private final PostFavoriteMapper postFavoriteMapper;
	private final UgcPostLikeRedisService ugcPostLikeRedisService;
	private final UgcPostFavoriteRedisService ugcPostFavoriteRedisService;
	private final FrontUgcFollowerStatService frontUgcFollowerStatService;

	public FrontUgcPostDetailEmbedService(
			PostMapper postMapper,
			PostOutsideLangReadService postOutsideLangReadService,
			MemberAccountService memberAccountService,
			UgcOfficialUserInfoReadService officialUserInfoReadService,
			TopicMapper topicMapper,
			BadgeMapper badgeMapper,
			BadgeDetailService badgeDetailService,
			UgcPostItemListForDetailService ugcPostItemListForDetailService,
			PostFavoriteMapper postFavoriteMapper,
			UgcPostLikeRedisService ugcPostLikeRedisService,
			UgcPostFavoriteRedisService ugcPostFavoriteRedisService,
			FrontUgcFollowerStatService frontUgcFollowerStatService) {
		this.postMapper = postMapper;
		this.postOutsideLangReadService = postOutsideLangReadService;
		this.memberAccountService = memberAccountService;
		this.officialUserInfoReadService = officialUserInfoReadService;
		this.topicMapper = topicMapper;
		this.badgeMapper = badgeMapper;
		this.badgeDetailService = badgeDetailService;
		this.ugcPostItemListForDetailService = ugcPostItemListForDetailService;
		this.postFavoriteMapper = postFavoriteMapper;
		this.ugcPostLikeRedisService = ugcPostLikeRedisService;
		this.ugcPostFavoriteRedisService = ugcPostFavoriteRedisService;
		this.frontUgcFollowerStatService = frontUgcFollowerStatService;
	}

	public Map<String, Object> buildPostInfoForMessage(long companyId, long postId, String langTag) {
		if (postId <= 0L) {
			return null;
		}
		Post row =
				postMapper.selectOne(
						new LambdaQueryWrapper<Post>()
								.eq(Post::getCompanyId, companyId)
								.eq(Post::getPostId, postId));
		if (row == null) {
			return null;
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>(PostCreateService.postToSnakeMap(row));
		postOutsideLangReadService.applyToRowMap(companyId, langTag, m);
		m.put("user_id_auth", "");
		applyFrontPostDetailFormat(m, row, companyId, langTag, 0L);
		return new LinkedHashMap<>(new TreeMap<>(m));
	}

	public Map<String, Object> buildPostInfoForH5Detail(
			long companyId, String postIdRaw, long visitorUserId, String langTag) {
		LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<Post>().eq(Post::getCompanyId, companyId);
		if (postIdRaw == null) {
			w.isNull(Post::getPostId);
		} else {
			String trimmed = postIdRaw.trim();
			if (trimmed.isEmpty()) {
				w.eq(Post::getPostId, 0L);
			} else {
				try {
					w.eq(Post::getPostId, Long.parseLong(trimmed));
				} catch (NumberFormatException e) {
					w.apply("post_id = {0}", trimmed);
				}
			}
		}
		Post row = postMapper.selectOne(w);
		if (row == null) {
			return null;
		}
		LinkedHashMap<String, Object> m = new LinkedHashMap<>(PostCreateService.postToSnakeMap(row));
		if (!ValuePresence.hasEffectiveValue(m.get("post_id"))) {
			return new LinkedHashMap<>(new TreeMap<>(m));
		}
		postOutsideLangReadService.applyToRowMap(companyId, langTag, m);
		m.put("user_id_auth", visitorUserId);
		applyFrontPostDetailFormat(m, row, companyId, langTag, visitorUserId);
		return new LinkedHashMap<>(new TreeMap<>(m));
	}

	public void applyFrontPostListRowFormat(
			LinkedHashMap<String, Object> m,
			Post entityRow,
			long companyId,
			String requestLangTag,
			long visitorUserId) {
		applyH5PostRowCommon(m, entityRow, companyId, requestLangTag, visitorUserId, false);
	}

	private void applyFrontPostDetailFormat(
			LinkedHashMap<String, Object> m,
			Post entityRow,
			long companyId,
			String requestLangTag,
			long visitorUserId) {
		applyH5PostRowCommon(m, entityRow, companyId, requestLangTag, visitorUserId, true);
	}

	private void applyH5PostRowCommon(
			LinkedHashMap<String, Object> m,
			Post entityRow,
			long companyId,
			String requestLangTag,
			long visitorUserId,
			boolean expandTopicsAndGoods) {
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
			if (member != null && !member.isEmpty()) {
				LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
				merged.putAll(member);
				if (wechat != null) {
					merged.putAll(wechat);
				}
				int source = parseIntLoose(m.get("source"));
				List<String> allowKeys =
						source == 2
								? List.of("username", "avatar", "headimgurl", "nickname", "user_id", "unionid")
								: List.of("username", "avatar", "headimgurl", "nickname", "user_id");
				Map<String, Object> userInfoOut = new TreeMap<>();
				for (String k : allowKeys) {
					if (merged.containsKey(k)) {
						userInfoOut.put(k, merged.get(k));
					}
				}
				m.put("userInfo", new LinkedHashMap<>(userInfoOut));
			} else {
				LinkedHashMap<String, Object> userInfoOut = new LinkedHashMap<>();
				if (wechat != null) {
					userInfoOut.putAll(wechat);
				}
				m.put("userInfo", userInfoOut);
			}
		} else {
			Map<String, String> official = officialUserInfoReadService.loadOfficialDisplay(companyId);
			LinkedHashMap<String, Object> ui = new LinkedHashMap<>();
			ui.put("nickname", official.getOrDefault("official.nickname", ""));
			ui.put("headimgurl", official.getOrDefault("official.headerimgurl", ""));
			m.put("userInfo", ui);
		}

		// H5 帖子列表：缺失 userInfo 时应序列化为 []；空 Map 在 Java 中序列化为 {}，需转为空数组
		if (!expandTopicsAndGoods) {
			Object uiObj = m.get("userInfo");
			if (uiObj instanceof Map<?, ?> um && um.isEmpty()) {
				m.put("userInfo", Collections.emptyList());
			}
		}

		if (expandTopicsAndGoods && ValuePresence.hasEffectiveValue(m.get("topics"))) {
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
											.eq(Topic::getStatus, 1)
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

		if (expandTopicsAndGoods && ValuePresence.hasEffectiveValue(m.get("goods"))) {
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
				log.debug("front post embed badges companyId={} badgeIdCount={}", companyId, ids.size());
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
						byId.put(b.getBadgeId(), trimBadgeForFrontDetailRow(br));
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

		Long postId = longOrNull(m.get("post_id"));
		Long bloggerUid = longOrNull(m.get("user_id"));
		if (visitorUserId > 0L && postId != null && postId > 0L) {
			m.put("like_status", ugcPostLikeRedisService.getUserPostLikeStatus(visitorUserId, postId));
			m.put("favorite_status", ugcPostFavoriteRedisService.getUserPostFavoriteStatus(visitorUserId, postId));
			int follow = 0;
			if (bloggerUid != null && bloggerUid > 0L) {
				follow = frontUgcFollowerStatService.readFollowStatus(visitorUserId, bloggerUid);
			}
			m.put("follow_status", follow);
		} else {
			m.put("like_status", 0);
			m.put("favorite_status", 0);
			m.put("follow_status", 0);
		}

		if (postId != null && postId > 0L) {
			LambdaQueryWrapper<PostFavorite> active =
					new LambdaQueryWrapper<PostFavorite>()
							.eq(PostFavorite::getPostId, postId)
							.eq(PostFavorite::getDisabled, false);
			long favCount = postFavoriteMapper.selectCount(active);
			m.put("favorite_nums", favCount);
		} else {
			m.put("favorite_nums", 0L);
		}

		applyScalarParity(m, entityRow);
	}

	private static LinkedHashMap<String, Object> trimBadgeForFrontDetailRow(Map<String, Object> src) {
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
