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
import cn.shopex.ecshopx.wsugc.domain.PostTopic;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostTopicMapper;
import cn.shopex.ecshopx.wsugc.service.content.UgcWxContentMediaCheckService;
import cn.shopex.ecshopx.wsugc.service.content.UgcWxContentTextCheckService;
import cn.shopex.ecshopx.wsugc.service.point.UgcPostPointService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FrontUgcPostCreateService {

	private static final Logger log = LoggerFactory.getLogger(FrontUgcPostCreateService.class);

	private final PostMapper postMapper;
	private final PostTopicMapper postTopicMapper;
	private final ObjectMapper objectMapper;
	private final UgcPostOpenIdResolveService ugcPostOpenIdResolveService;
	private final UgcWxContentTextCheckService ugcWxContentTextCheckService;
	private final UgcWxContentMediaCheckService ugcWxContentMediaCheckService;
	private final UgcPostPointService ugcPostPointService;
	private final String importImagePublicBase;

	public FrontUgcPostCreateService(
			PostMapper postMapper,
			PostTopicMapper postTopicMapper,
			ObjectMapper objectMapper,
			UgcPostOpenIdResolveService ugcPostOpenIdResolveService,
			UgcWxContentTextCheckService ugcWxContentTextCheckService,
			UgcWxContentMediaCheckService ugcWxContentMediaCheckService,
			UgcPostPointService ugcPostPointService,
			@Value("${ecshopx.filesystem.import-image-public-base:}") String importImagePublicBase) {
		this.postMapper = postMapper;
		this.postTopicMapper = postTopicMapper;
		this.objectMapper = objectMapper;
		this.ugcPostOpenIdResolveService = ugcPostOpenIdResolveService;
		this.ugcWxContentTextCheckService = ugcWxContentTextCheckService;
		this.ugcWxContentMediaCheckService = ugcWxContentMediaCheckService;
		this.ugcPostPointService = ugcPostPointService;
		this.importImagePublicBase = importImagePublicBase;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createOrUpdateByMember(
			Map<String, Object> input, long userId, String mobile, long companyId, String clientIp) {
		if (userId <= 0L) {
			throw new ResourceException("只有会员才可以发布笔记");
		}

		int isDraft = 0;
		if (input.containsKey("is_draft")) {
			Integer id = parseIntLoose(input.get("is_draft"));
			if (id != null && id == 1) {
				isDraft = 1;
			}
		}

		if (isDraft != 1) {
			Object cover = input.get("cover");
			if (cover == null || !StringUtils.hasText(cover.toString().trim())) {
				throw new ResourceException("封面图不能为空");
			}
			if (!input.containsKey("images") || !(input.get("images") instanceof List<?> il) || il.isEmpty()) {
				throw new ResourceException("images参数不能为空");
			}
			if (!input.containsKey("image_path") || !(input.get("image_path") instanceof List<?> pl) || pl.isEmpty()) {
				throw new ResourceException("image_path参数不能为空");
			}
		}

		Long requestPostId = parseOptionalPostId(input);
		boolean isEdit = requestPostId != null;
		Post existing = null;
		if (isEdit) {
			existing = postMapper.selectById(requestPostId);
			if (existing == null) {
				throw new ResourceException("笔记不存在");
			}
			if (!Objects.equals(existing.getUserId(), userId)) {
				throw new ResourceException("非法操作：不能编辑别人的笔记");
			}
		}

		int now = (int) (System.currentTimeMillis() / 1000);
		String openId = ugcPostOpenIdResolveService.resolveOpenId(userId, companyId);

		String title = resolveTextField(input, isEdit, existing, true);
		String content = resolveTextField(input, isEdit, existing, false);

		int titleStatus;
		if (!StringUtils.hasText(title)) {
			titleStatus = 1;
		} else {
			titleStatus = ugcWxContentTextCheckService.checkTextStatus(companyId, title, openId);
		}

		int contentStatus;
		if (!StringUtils.hasText(content)) {
			contentStatus = 1;
		} else {
			contentStatus = ugcWxContentTextCheckService.checkTextStatus(companyId, content, openId);
		}

		List<String> imagePaths = extractImagePaths(input);
		int imageStatus;
		List<Map<String, Object>> mediaCheckTraceId = new ArrayList<>();
		List<String> traceIdsSegments = new ArrayList<>();

		if (imagePaths == null || imagePaths.isEmpty()) {
			imageStatus = 1;
		} else {
			imageStatus = 0;
			int idx = 0;
			for (String path : imagePaths) {
				idx++;
				String fullUrl = buildImportImageUrl(path);
				String traceId = ugcWxContentMediaCheckService.checkMediaAsyncTraceId(companyId, fullUrl, openId);
				if (StringUtils.hasText(traceId)) {
					Map<String, Object> item = new LinkedHashMap<>();
					item.put("trace_id", traceId);
					item.put("image_full_url", fullUrl);
					item.put("image_index", idx);
					mediaCheckTraceId.add(item);
					traceIdsSegments.add("," + traceId + ":false");
				}
			}
		}

		int aggregatedStatus;
		if (titleStatus == 1 && contentStatus == 1 && imageStatus == 1) {
			aggregatedStatus = 1;
		} else if (titleStatus == 4 || contentStatus == 4 || imageStatus == 4) {
			aggregatedStatus = 4;
		} else {
			aggregatedStatus = 0;
		}
		if (isDraft == 1) {
			aggregatedStatus = 0;
		}

		Post post;
		if (isEdit) {
			post = existing;
		} else {
			post = new Post();
			post.setLikes(0);
			post.setShareNums(0);
		}

		applyScalarString(post, input, "title");
		applyScalarString(post, input, "content");
		applyScalarString(post, input, "cover");
		applyScalarString(post, input, "video");
		applyScalarString(post, input, "video_ratio");
		applyScalarString(post, input, "video_place");
		applyScalarString(post, input, "video_thumb");

		applyCommaOrNullTopicsBadgesGoods(post, input, "topics");
		applyCommaOrNullTopicsBadgesGoods(post, input, "badges");
		applyCommaOrNullTopicsBadgesGoods(post, input, "goods");

		applyImagesJson(post, input);
		applyImagePath(post, input, isDraft);
		applyImageTag(post, input);

		post.setUserId(userId);
		post.setMobile(mobile);
		post.setCompanyId(companyId);
		post.setEnabled(1);
		post.setIp(clientIp != null ? clientIp : "");
		post.setOperatorId(0L);
		post.setSource(1);
		post.setIsDraft(isDraft);
		post.setTitleStatus(titleStatus);
		post.setContentStatus(contentStatus);
		post.setImageStatus(imageStatus);
		post.setStatus(aggregatedStatus);

		post.setIsTop(0);

		Integer pOrderParsed = null;
		if (input.containsKey("p_order") && input.get("p_order") != null) {
			pOrderParsed = parseIntLoose(input.get("p_order"));
		}
		post.setPOrder(pOrderParsed != null ? pOrderParsed : 0);

		long postId;
		if (isEdit) {
			post.setUpdated(now);
			if (postMapper.updateById(post) <= 0) {
				throw new ResourceException("未查询到更新数据");
			}
			postId = post.getPostId();
		} else {
			post.setCreated(now);
			post.setUpdated(now);
			postMapper.insert(post);
			postId = post.getPostId();
		}

		if (!mediaCheckTraceId.isEmpty()) {
			String mediacheckJson;
			try {
				mediacheckJson = objectMapper.writeValueAsString(mediaCheckTraceId);
			} catch (JsonProcessingException e) {
				throw new ResourceException("参数格式无效");
			}
			String traceJoined = String.join("|", traceIdsSegments);
			LambdaUpdateWrapper<Post> uw = new LambdaUpdateWrapper<>();
			uw.eq(Post::getPostId, postId)
					.set(Post::getMediacheckTraceid, mediacheckJson)
					.set(Post::getTraceIds, traceJoined);
			if (postMapper.update(null, uw) <= 0) {
				throw new ResourceException("未查询到更新数据");
			}
		}

		if (input.containsKey("topics")) {
			@SuppressWarnings("unchecked")
			List<String> tl = (List<String>) input.get("topics");
			if (!tl.isEmpty()) {
				String topicsCsv = ElementJoiner.joinComma(tl);
				postTopicMapper.delete(
						new LambdaQueryWrapper<PostTopic>().eq(PostTopic::getPostId, postId));
				for (String seg : topicsCsv.split(",")) {
					if (StringUtils.hasText(seg)) {
						PostTopic row = new PostTopic();
						row.setPostId(postId);
						row.setTopicId(seg.trim());
						row.setCompanyId(companyId);
						row.setCreated(now);
						postTopicMapper.insert(row);
					}
				}
			}
		}

		Post fresh = postMapper.selectById(postId);
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}

		LinkedHashMap<String, Object> data = new LinkedHashMap<>(PostCreateService.postToSnakeMap(fresh));
		normalizeH5CreatePostOptionalNulls(data);
		String message;
		if (isDraft == 1) {
			message = "草稿已保存";
		} else {
			int st = fresh.getStatus() != null ? fresh.getStatus() : 0;
			if (st == 1) {
				message = "发布成功";
			} else if (st == 4) {
				message = "笔记内容违规，审核失败，请修改后重新提交";
			} else {
				message = "笔记已提交，人工审核中";
			}
		}
		data.put("message", message);

		if (!isEdit && isDraft == 0) {
			int st = fresh.getStatus() != null ? fresh.getStatus() : 0;
			if (st == 1) {
				try {
					ugcPostPointService.addUgcPoint(postId, userId, companyId, 20, null);
				} catch (Exception e) {
					log.debug("ugc post create point skipped: {}", e.toString(), e);
				}
			}
		}

		return data;
	}

	private List<String> extractImagePaths(Map<String, Object> input) {
		if (!input.containsKey("image_path")) {
			return null;
		}
		Object raw = input.get("image_path");
		if (!(raw instanceof List<?> lp)) {
			return null;
		}
		List<String> out = new ArrayList<>();
		for (Object o : lp) {
			if (o == null) {
				continue;
			}
			String s = o.toString().trim();
			if (s.isEmpty()) {
				continue;
			}
			out.add(s);
		}
		return out;
	}

	private String buildImportImageUrl(String path) {
		String p = path == null ? "" : path.trim();
		if (!StringUtils.hasText(importImagePublicBase)) {
			return p.startsWith("/") ? p : "/" + p;
		}
		String base = importImagePublicBase.replaceAll("/+$", "");
		String rel = p.replaceAll("^/+", "");
		return base + "/" + rel;
	}

	private void applyScalarString(Post post, Map<String, Object> input, String key) {
		if (input.containsKey(key)) {
			Object v = input.get(key);
			switch (key) {
				case "title" -> post.setTitle(v == null ? null : v.toString());
				case "content" -> post.setContent(v == null ? null : v.toString());
				case "cover" -> post.setCover(v == null ? null : v.toString());
				case "video" -> post.setVideo(v == null ? null : v.toString());
				case "video_ratio" -> post.setVideoRatio(v == null ? null : v.toString());
				case "video_place" -> post.setVideoPlace(v == null ? null : v.toString());
				case "video_thumb" -> post.setVideoThumb(v == null ? null : v.toString());
				default -> { }
			}
		}
	}

	private void applyCommaOrNullTopicsBadgesGoods(Post post, Map<String, Object> input, String key) {
		if (input.containsKey(key)) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) input.get(key);
			if (list.isEmpty()) {
				switch (key) {
					case "topics" -> post.setTopics(null);
					case "badges" -> post.setBadges(null);
					case "goods" -> post.setGoods(null);
					default -> { }
				}
			} else {
				String csv = ElementJoiner.joinComma(list);
				switch (key) {
					case "topics" -> post.setTopics(csv);
					case "badges" -> post.setBadges(csv);
					case "goods" -> post.setGoods(csv);
					default -> { }
				}
			}
		}
	}

	private void applyImagesJson(Post post, Map<String, Object> input) {
		if (input.containsKey("images")) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) input.get("images");
			try {
				if (list.isEmpty()) {
					post.setImages(objectMapper.writeValueAsString(Collections.emptyList()));
				} else {
					post.setImages(objectMapper.writeValueAsString(list));
				}
			} catch (JsonProcessingException e) {
				throw new ResourceException("参数格式无效（images）");
			}
		}
	}

	private void applyImagePath(Post post, Map<String, Object> input, int isDraft) {
		if (input.containsKey("image_path")) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) input.get("image_path");
			if (!list.isEmpty()) {
				post.setImagePath(ElementJoiner.joinComma(list));
			} else if (isDraft == 1) {
				// draft: do not set column
			}
		}
	}

	private void applyImageTag(Post post, Map<String, Object> input) {
		if (input.containsKey("image_tag")) {
			Object tag = input.get("image_tag");
			if (tag == null || isEmptyTagStructure(tag)) {
				post.setImageTag(null);
			} else {
				try {
					post.setImageTag(objectMapper.writeValueAsString(tag));
				} catch (JsonProcessingException e) {
					throw new ResourceException("image_tag 格式无效");
				}
			}
		}
	}

	private static boolean isEmptyTagStructure(Object tag) {
		if (tag == null) {
			return true;
		}
		if (tag instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (tag instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (tag instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		if (tag instanceof Object[] a) {
			return a.length == 0;
		}
		return false;
	}

	private static String resolveTextField(Map<String, Object> input, boolean isEdit, Post existing, boolean titleKey) {
		String key = titleKey ? "title" : "content";
		if (input.containsKey(key)) {
			Object v = input.get(key);
			return v == null ? null : v.toString();
		}
		if (isEdit) {
			return titleKey ? existing.getTitle() : existing.getContent();
		}
		return null;
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
			if (id <= 0L) {
				return null;
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException("post_id 格式无效");
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

	/**
	 * Legacy payloads may omit counters and verify timestamps; persistence uses numeric zero defaults.
	 * Only for H5 create response — do not change {@link PostCreateService#postToSnakeMap}.
	 */
	private static void normalizeH5CreatePostOptionalNulls(Map<String, Object> data) {
		for (String key : new String[] {"share_nums", "ai_verify_time", "manual_verify_time"}) {
			Object v = data.get(key);
			if (v == null || (v instanceof Number n && n.longValue() == 0L)) {
				data.put(key, null);
			}
		}
	}
}
