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
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.domain.PostBadge;
import cn.shopex.ecshopx.wsugc.domain.PostTopic;
import cn.shopex.ecshopx.wsugc.mapper.PostBadgeMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostTopicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PostCreateService {

	private final PostMapper postMapper;
	private final PostTopicMapper postTopicMapper;
	private final PostBadgeMapper postBadgeMapper;
	private final ObjectMapper objectMapper;
	private final PostTopCapService postTopCapService;

	public PostCreateService(
			PostMapper postMapper,
			PostTopicMapper postTopicMapper,
			PostBadgeMapper postBadgeMapper,
			ObjectMapper objectMapper,
			PostTopCapService postTopCapService) {
		this.postMapper = postMapper;
		this.postTopicMapper = postTopicMapper;
		this.postBadgeMapper = postBadgeMapper;
		this.objectMapper = objectMapper;
		this.postTopCapService = postTopCapService;
	}

	public Map<String, Object> createOrUpdate(
			Map<String, Object> input,
			Map<String, Object> operatorJwtOrEmpty,
			String clientIp,
			boolean localProfile) {
		LinkedHashMap<String, Object> persist = new LinkedHashMap<>();

		long operatorIdFromAuth = 0L;
		if (readLong(operatorJwtOrEmpty.get("operator_id"), 0L) != 0L) {
			operatorIdFromAuth = readLong(operatorJwtOrEmpty.get("operator_id"), 0L);
		} else if (localProfile && input.containsKey("user_id")) {
			operatorIdFromAuth = readLong(input.get("user_id"), 0L);
		}

		copyWhitelistPresent(input, persist);

		Object coverVal = persist.get("cover");
		if (coverVal == null || !StringUtils.hasText(coverVal.toString().trim())) {
			throw new ResourceException("封面图不能为空");
		}

		if (input.containsKey("images")) {
			Object im = input.get("images");
			if (isEmptyImages(im)) {
				throw new ResourceException("images参数不能为空");
			}
			if (im instanceof Collection<?> || im != null && im.getClass().isArray()) {
				try {
					persist.put("images", objectMapper.writeValueAsString(im));
				} catch (JsonProcessingException e) {
					throw new ResourceException("images参数不能为空");
				}
			} else {
				persist.put("images", im.toString());
			}
		}

		if (input.containsKey("topics")) {
			Object t = input.get("topics");
			if (isEmptyCollectionOrArray(t)) {
				persist.put("topics", null);
			} else {
				persist.put("topics", ElementJoiner.joinComma(t));
			}
		}
		if (input.containsKey("badges")) {
			Object t = input.get("badges");
			if (isEmptyCollectionOrArray(t)) {
				persist.put("badges", null);
			} else {
				persist.put("badges", ElementJoiner.joinComma(t));
			}
		}
		if (input.containsKey("goods")) {
			Object t = input.get("goods");
			if (isEmptyCollectionOrArray(t)) {
				persist.put("goods", null);
			} else {
				persist.put("goods", ElementJoiner.joinComma(t));
			}
		}

		Object imagePathIn = input.get("image_path");
		if (!input.containsKey("image_path")
				|| imagePathIn == null
				|| !isNonEmptyCollectionOrArray(imagePathIn)) {
			throw new ResourceException("image_path参数不能为空");
		}
		persist.put("image_path", ElementJoiner.joinComma(imagePathIn));

		if (input.containsKey("image_tag")) {
			Object tag = input.get("image_tag");
			if (isEmptyCollectionOrArray(tag)) {
				persist.put("image_tag", null);
			} else {
				try {
					persist.put("image_tag", objectMapper.writeValueAsString(tag));
				} catch (JsonProcessingException e) {
					throw new ResourceException("image_tag 格式无效");
				}
			}
		}

		persist.put("source", 2);
		persist.put("operator_id", operatorIdFromAuth);
		persist.put("user_id", 0L);
		persist.put("mobile", readString(operatorJwtOrEmpty.get("mobile"), "0"));
		persist.put("company_id", readLong(operatorJwtOrEmpty.get("company_id"), 1L));
		persist.put("enabled", 1);
		persist.put("status", 1);
		persist.put("ip", clientIp != null ? clientIp : "");

		int pOrderEarly = 0;
		if (input.containsKey("p_order") && input.get("p_order") != null) {
			Integer po = parseIntLoose(input.get("p_order"));
			if (po != null) {
				pOrderEarly = po;
			}
		}
		persist.put("p_order", pOrderEarly);

		int tmpParamsTop = 0;
		if (input.containsKey("is_top") && input.get("is_top") != null) {
			if (isTruthyIsTopOne(input.get("is_top"))) {
				tmpParamsTop = 1;
				persist.put("is_top", 0);
				persist.put("p_order", 0);
			} else {
				persist.put("is_top", 0);
			}
		} else {
			persist.put("is_top", 0);
		}

		int isTopNow = toIntValue(persist.get("is_top"), 0);
		if (isTopNow == 0) {
			int po = toIntValue(persist.get("p_order"), 0);
			if (po <= 0) {
				persist.put("p_order", 0);
			}
		}

		Long requestPostId = parseOptionalPostId(input);
		boolean isEdit = requestPostId != null;
		int now = (int) (System.currentTimeMillis() / 1000);

		long companyIdForRel = readLong(operatorJwtOrEmpty.get("company_id"), 1L);

		if (isEdit) {
			Post existing = postMapper.selectById(requestPostId);
			if (existing == null) {
				throw new ResourceException("未查询到更新数据");
			}
			applyPersistToPost(existing, persist);
			existing.setUpdated(now);
			postMapper.updateById(existing);
		} else {
			Post entity = new Post();
			applyPersistToPost(entity, persist);
			entity.setCreated(now);
			entity.setUpdated(now);
			postMapper.insert(entity);
			requestPostId = entity.getPostId();
		}

		Long postId = requestPostId;
		String topicsStr = persist.containsKey("topics") ? (String) persist.get("topics") : null;
		String badgesStr = persist.containsKey("badges") ? (String) persist.get("badges") : null;

		if (postId != null) {
			if (StringUtils.hasText(topicsStr)) {
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
			} else {
				postTopicMapper.delete(
						new LambdaQueryWrapper<PostTopic>().eq(PostTopic::getPostId, postId));
			}

			if (StringUtils.hasText(badgesStr)) {
				postBadgeMapper.delete(
						new LambdaQueryWrapper<PostBadge>().eq(PostBadge::getPostId, postId));
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
							// 非法 badge_id 段跳过，不中断本次保存
						}
					}
				}
			} else {
				postBadgeMapper.delete(
						new LambdaQueryWrapper<PostBadge>().eq(PostBadge::getPostId, postId));
			}

			if (tmpParamsTop == 1) {
				postTopCapService.applyTopCapAfterRequest(postId);
			}
		}

		Post fresh = postMapper.selectById(postId);
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}

		TreeMap<String, Object> sorted = new TreeMap<>(postToSnakeMap(fresh));
		LinkedHashMap<String, Object> data = new LinkedHashMap<>(sorted);
		data.put(
				"message",
				isEdit ? "更新笔记成功" : "创建笔记成功");
		return data;
	}

	private static void copyWhitelistPresent(Map<String, Object> input, LinkedHashMap<String, Object> persist) {
		String[] keys = {
			"post_id", "user_id", "title", "content", "cover", "images", "image_tag", "image_path",
			"topics", "video", "video_ratio", "video_place", "video_thumb", "is_draft", "badges", "goods",
			"is_top", "p_order"
		};
		for (String k : keys) {
			if (input.containsKey(k)) {
				persist.put(k, input.get(k));
			}
		}
	}

	public static Map<String, Object> postToSnakeMap(Post p) {
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
		m.put("ai_refuse_reason", p.getAiRefuseReason());
		m.put("manual_refuse_reason", p.getManualRefuseReason());
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

	private void applyPersistToPost(Post p, Map<String, Object> persist) {
		for (Map.Entry<String, Object> e : persist.entrySet()) {
			String k = e.getKey();
			Object v = e.getValue();
			switch (k) {
				case "post_id" -> { /* handled by flow */ }
				case "title" -> p.setTitle(v != null ? v.toString() : null);
				case "content" -> p.setContent(v != null ? v.toString() : null);
				case "cover" -> p.setCover(v != null ? v.toString() : null);
				case "images" -> p.setImages(v != null ? v.toString() : null);
				case "image_tag" -> p.setImageTag(v != null ? v.toString() : null);
				case "image_path" -> p.setImagePath(v != null ? v.toString() : null);
				case "topics" -> p.setTopics(v != null ? v.toString() : null);
				case "badges" -> p.setBadges(v != null ? v.toString() : null);
				case "goods" -> p.setGoods(v != null ? v.toString() : null);
				case "video" -> p.setVideo(v != null ? v.toString() : null);
				case "video_ratio" -> p.setVideoRatio(v != null ? v.toString() : null);
				case "video_place" -> p.setVideoPlace(v != null ? v.toString() : null);
				case "video_thumb" -> p.setVideoThumb(v != null ? v.toString() : null);
				case "is_draft" -> p.setIsDraft(parseIntLoose(v));
				case "source" -> p.setSource(parseIntLoose(v));
				case "operator_id" -> p.setOperatorId(toLong(v));
				case "user_id" -> p.setUserId(toLong(v));
				case "mobile" -> p.setMobile(v != null ? v.toString() : null);
				case "company_id" -> p.setCompanyId(toLong(v));
				case "enabled" -> p.setEnabled(parseIntLoose(v));
				case "status" -> p.setStatus(parseIntLoose(v));
				case "ip" -> p.setIp(v != null ? v.toString() : null);
				case "is_top" -> p.setIsTop(parseIntLoose(v));
				case "p_order" -> p.setPOrder(parseIntLoose(v));
				default -> { }
			}
		}
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

	private static int toIntValue(Object v, int defaultVal) {
		Integer i = parseIntLoose(v);
		return i != null ? i : defaultVal;
	}

	private static Long parseOptionalPostId(Map<String, Object> input) {
		if (!input.containsKey("post_id")) {
			return null;
		}
		Object v = input.get("post_id");
		if (v == null) {
			return null;
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long id = Long.parseLong(s);
			if (id <= 0) {
				return null;
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException("post_id 格式无效");
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

	private static String readString(Object v, String defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		return v.toString();
	}

	private static boolean isEmptyImages(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Object[] a) {
			return a.length == 0;
		}
		if (v instanceof int[] a) {
			return a.length == 0;
		}
		if (v instanceof long[] a) {
			return a.length == 0;
		}
		return false;
	}

	private static boolean isEmptyCollectionOrArray(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Object[] a) {
			return a.length == 0;
		}
		if (v instanceof int[] a) {
			return a.length == 0;
		}
		if (v instanceof long[] a) {
			return a.length == 0;
		}
		return false;
	}

	private static boolean isNonEmptyCollectionOrArray(Object v) {
		return !isEmptyCollectionOrArray(v)
				&& (v instanceof Collection<?> || v != null && v.getClass().isArray());
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
