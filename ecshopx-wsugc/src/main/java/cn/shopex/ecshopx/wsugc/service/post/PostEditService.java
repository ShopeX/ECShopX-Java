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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.ElementJoiner;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.domain.PostBadge;
import cn.shopex.ecshopx.wsugc.domain.PostTopic;
import cn.shopex.ecshopx.wsugc.mapper.PostBadgeMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostTopicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PostEditService {

	private final PostMapper postMapper;
	private final PostTopicMapper postTopicMapper;
	private final PostBadgeMapper postBadgeMapper;
	private final PostTopCapService postTopCapService;

	public PostEditService(
			PostMapper postMapper,
			PostTopicMapper postTopicMapper,
			PostBadgeMapper postBadgeMapper,
			PostTopCapService postTopCapService) {
		this.postMapper = postMapper;
		this.postTopicMapper = postTopicMapper;
		this.postBadgeMapper = postBadgeMapper;
		this.postTopCapService = postTopCapService;
	}

	public Map<String, Object> edit(
			Map<String, Object> input,
			Map<String, Object> operatorJwtOrEmpty,
			boolean localProfile) {
		long postId = parseRequiredPostId(input);

		long operatorId = readLong(operatorJwtOrEmpty.get("operator_id"), 0L);
		if (operatorId == 0L && localProfile && input.containsKey("user_id")) {
			operatorId = readLong(input.get("user_id"), 0L);
		}

		LinkedHashMap<String, Object> persist = new LinkedHashMap<>();

		if (input.containsKey("topics") && ValuePresence.hasEffectiveValue(input.get("topics"))) {
			persist.put("topics", ElementJoiner.joinComma(input.get("topics")));
		}
		if (input.containsKey("badges") && ValuePresence.hasEffectiveValue(input.get("badges"))) {
			persist.put("badges", ElementJoiner.joinComma(input.get("badges")));
		}

		int pOrderEarly = 0;
		if (input.containsKey("p_order") && input.get("p_order") != null) {
			Integer po = parseIntLoose(input.get("p_order"));
			if (po != null) {
				pOrderEarly = po;
			}
		}

		int tmpParamsTop = 0;
		int isTopForDb = 0;
		int pOrderForDb = pOrderEarly;

		if (input.containsKey("is_top") && input.get("is_top") != null) {
			if (isTruthyIsTopOne(input.get("is_top"))) {
				tmpParamsTop = 1;
				isTopForDb = 0;
				pOrderForDb = 0;
			} else {
				isTopForDb = 0;
			}
		} else {
			isTopForDb = 0;
		}

		persist.put("p_order", pOrderForDb);
		persist.put("is_top", isTopForDb);
		persist.put("operator_id", operatorId);

		Post existing = postMapper.selectById(postId);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}

		int now = (int) (System.currentTimeMillis() / 1000);
		applyPersistToPostForEdit(existing, persist);
		existing.setUpdated(now);
		postMapper.updateById(existing);

		long companyIdForRel = readLong(operatorJwtOrEmpty.get("company_id"), 1L);

		if (persist.containsKey("topics") && StringUtils.hasText((String) persist.get("topics"))) {
			String topicsStr = (String) persist.get("topics");
			postTopicMapper.delete(
					new LambdaQueryWrapper<PostTopic>().eq(PostTopic::getPostId, postId));
			for (String seg : topicsStr.split(",")) {
				if (StringUtils.hasText(seg)) {
					PostTopic row = new PostTopic();
					row.setPostId(postId);
					row.setTopicId(seg.trim());
					row.setCompanyId(companyIdForRel);
					row.setCreated(now);
					postTopicMapper.insert(row);
				}
			}
		}

		if (input.containsKey("badges") && ValuePresence.hasEffectiveValue(input.get("badges"))) {
			postBadgeMapper.delete(
					new LambdaQueryWrapper<PostBadge>().eq(PostBadge::getPostId, postId));
			String badgesStr = (String) persist.get("badges");
			if (StringUtils.hasText(badgesStr)) {
				for (String seg : badgesStr.split(",")) {
					if (StringUtils.hasText(seg)) {
						try {
							long bid = Long.parseLong(seg.trim());
							PostBadge row = new PostBadge();
							row.setPostId(postId);
							row.setBadgeId(bid);
							row.setCompanyId(companyIdForRel);
							row.setCreated(now);
							postBadgeMapper.insert(row);
						} catch (NumberFormatException ignored) {
							// 非法 badge_id 段跳过
						}
					}
				}
			}
		}

		if (tmpParamsTop == 1) {
			postTopCapService.applyTopCapAfterRequest(postId);
		}

		Post fresh = postMapper.selectById(postId);
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}

		TreeMap<String, Object> sorted = new TreeMap<>(postToSnakeMap(fresh));
		LinkedHashMap<String, Object> data = new LinkedHashMap<>(sorted);
		data.put("message", "更新笔记成功");
		return data;
	}

	private static long parseRequiredPostId(Map<String, Object> input) {
		if (!input.containsKey("post_id")) {
			throw new ResourceException("post_id不能为空");
		}
		Object v = input.get("post_id");
		if (v == null) {
			throw new ResourceException("post_id不能为空");
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("post_id不能为空");
		}
		try {
			long id = Long.parseLong(s);
			if (id <= 0) {
				throw new ResourceException("post_id不能为空");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException("post_id 格式无效");
		}
	}

	private static void applyPersistToPostForEdit(Post p, Map<String, Object> persist) {
		for (Map.Entry<String, Object> e : persist.entrySet()) {
			String k = e.getKey();
			Object v = e.getValue();
			switch (k) {
				case "topics" -> p.setTopics(v != null ? v.toString() : null);
				case "badges" -> p.setBadges(v != null ? v.toString() : null);
				case "operator_id" -> p.setOperatorId(toLong(v));
				case "is_top" -> p.setIsTop(parseIntLoose(v));
				case "p_order" -> p.setPOrder(parseIntLoose(v));
				default -> { }
			}
		}
	}

	private static Map<String, Object> postToSnakeMap(Post p) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("post_id", p.getPostId());
		m.put("user_id", p.getUserId() != null ? p.getUserId().intValue() : 0);
		m.put("title", p.getTitle());
		m.put("likes", p.getLikes());
		m.put("ip", p.getIp());
		m.put("mobile", p.getMobile());
		m.put("cover", p.getCover());
		m.put("video", p.getVideo());
		m.put("video_ratio", p.getVideoRatio());
		m.put("video_place", p.getVideoPlace());
		m.put("video_thumb", p.getVideoThumb());
		m.put("position", p.getPosition());
		m.put("address", p.getAddress());
		m.put("topics", p.getTopics());
		m.put("badges", p.getBadges());
		m.put("goods", p.getGoods());
		m.put("images", p.getImages());
		m.put("content", p.getContent());
		m.put("p_order", p.getPOrder());
		m.put("view_auth", p.getViewAuth());
		m.put("is_draft", p.getIsDraft());
		m.put("enabled", p.getEnabled());
		m.put("disabled", p.getDisabled());
		m.put("status", p.getStatus());
		m.put("company_id", p.getCompanyId());
		m.put("created", p.getCreated());
		m.put("updated", p.getUpdated());
		m.put("ai_verify_time", p.getAiVerifyTime());
		m.put("manual_verify_time", p.getManualVerifyTime());
		m.put("image_path", p.getImagePath());
		m.put("image_tag", p.getImageTag());
		m.put("operator_id", p.getOperatorId());
		m.put("source", p.getSource());
		m.put("is_top", p.getIsTop());
		m.put("share_nums", p.getShareNums());
		m.put("title_status", p.getTitleStatus());
		m.put("content_status", p.getContentStatus());
		m.put("image_status", p.getImageStatus());
		m.put("mediacheck_traceid", p.getMediacheckTraceid());
		m.put("trace_ids", p.getTraceIds());
		return m;
	}

	private static Long toLong(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer parseIntLoose(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
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

	private static boolean isTruthyIsTopOne(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Number n) {
			return n.intValue() == 1;
		}
		return "1".equals(v.toString().trim());
	}
}
