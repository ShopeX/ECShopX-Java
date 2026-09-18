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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatOfficialAccountMaterialNewsService {

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final ObjectMapper objectMapper;

	public WechatOfficialAccountMaterialNewsService(WxJavaMpRuntime wxJavaMpRuntime, ObjectMapper objectMapper) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> createNews(String authorizerAppid, List<Map<String, Object>> articles) {
		if (articles == null) {
			throw new BadRequestException("请填写图文参数");
		}
		if (articles.size() >= 9) {
			throw new ResourceException("最多添加8个图文");
		}

		ArrayNode articlesNode = objectMapper.createArrayNode();
		for (Map<String, Object> row : articles) {
			ObjectNode article = objectMapper.createObjectNode();
			String title = coerceString(row, "title");
			String author = coerceString(row, "author");
			String content = coerceString(row, "content");
			String thumbMediaId = coerceString(row, "thumb_media_id");
			if (!StringUtils.hasText(title)
					|| !StringUtils.hasText(author)
					|| !StringUtils.hasText(content)
					|| !StringUtils.hasText(thumbMediaId)) {
				throw new ResourceException("请填写完整的图文消息");
			}
			article.put("title", title);
			article.put("author", author);
			article.put("content", content);
			article.put("thumb_media_id", thumbMediaId);
			String digest = coerceString(row, "digest");
			article.put("digest", StringUtils.hasText(digest) ? digest : "");
			String contentSourceUrl = coerceString(row, "content_source_url");
			article.put("content_source_url", StringUtils.hasText(contentSourceUrl) ? contentSourceUrl : "");
			article.put("show_cover_pic", parseShowCoverPic(row.get("show_cover_pic")));
			articlesNode.add(article);
		}

		ObjectNode rootObject = objectMapper.createObjectNode();
		rootObject.set("articles", articlesNode);

		Map<String, Object> payload =
				objectMapper.convertValue(rootObject, new TypeReference<LinkedHashMap<String, Object>>() {});
		String responseBody;
		try {
			responseBody =
					wxJavaMpRuntime
							.mp(authorizerAppid.trim())
							.post(WxMpApiUrl.Material.NEWS_ADD_URL, payload);
		} catch (WxErrorException e) {
			throw new ResourceException(resolveWechatFailureMessage(e));
		}
		if (!StringUtils.hasText(responseBody)) {
			throw new ResourceException("微信接口调用失败");
		}

		Map<String, Object> parsed = parseWechatSuccessMap(responseBody);
		Object mediaIdObj = parsed.get("media_id");
		if (mediaIdObj == null) {
			throw new ResourceException("微信接口调用失败");
		}
		String mediaId = mediaIdObj instanceof String s ? s : String.valueOf(mediaIdObj);
		if (!StringUtils.hasText(mediaId)) {
			throw new ResourceException("微信接口调用失败");
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("media_id", mediaId.trim());
		return out;
	}

	public void updateArticle(String authorizerAppid, String mediaId, List<Map<String, Object>> articles) {
		if (articles == null) {
			throw new BadRequestException("请填写图文参数");
		}
		if (articles.size() >= 9) {
			throw new ResourceException("最多添加8个图文");
		}
		if (!StringUtils.hasText(mediaId)) {
			throw new BadRequestException("请传入图文素材ID");
		}
		String mediaIdTrimmed = mediaId.trim();
		for (int index = 0; index < articles.size(); index++) {
			Map<String, Object> row = articles.get(index);
			ObjectNode article = objectMapper.createObjectNode();
			String title = coerceString(row, "title");
			String author = coerceString(row, "author");
			String content = coerceString(row, "content");
			String thumbMediaId = coerceString(row, "thumb_media_id");
			if (!StringUtils.hasText(title)
					|| !StringUtils.hasText(author)
					|| !StringUtils.hasText(content)
					|| !StringUtils.hasText(thumbMediaId)) {
				throw new ResourceException("请填写完整的图文消息");
			}
			article.put("title", title);
			article.put("author", author);
			article.put("content", content);
			article.put("thumb_media_id", thumbMediaId);
			String digest = coerceString(row, "digest");
			article.put("digest", StringUtils.hasText(digest) ? digest : "");
			String contentSourceUrl = coerceString(row, "content_source_url");
			article.put("content_source_url", StringUtils.hasText(contentSourceUrl) ? contentSourceUrl : "");
			article.put("show_cover_pic", parseShowCoverPic(row.get("show_cover_pic")));

			ObjectNode rootObject = objectMapper.createObjectNode();
			rootObject.put("media_id", mediaIdTrimmed);
			rootObject.put("index", index);
			rootObject.set("articles", article);

			Map<String, Object> payload =
					objectMapper.convertValue(rootObject, new TypeReference<LinkedHashMap<String, Object>>() {});
			String responseBody;
			try {
				responseBody =
						wxJavaMpRuntime
								.mp(authorizerAppid.trim())
								.post(WxMpApiUrl.Material.NEWS_UPDATE_URL, payload);
			} catch (WxErrorException e) {
				throw new ResourceException(resolveWechatFailureMessage(e));
			}
			if (!StringUtils.hasText(responseBody)) {
				throw new ResourceException("微信接口调用失败");
			}
			parseWechatSuccessMap(responseBody);
		}
	}

	private static String coerceString(Map<String, Object> row, String key) {
		Object v = row.get(key);
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}

	private static int parseShowCoverPic(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Boolean b) {
			return Boolean.TRUE.equals(b) ? 1 : 0;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0 ? 1 : 0;
		}
		if (raw instanceof CharSequence cs) {
			String s = cs.toString().trim();
			if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
				return 1;
			}
			return 0;
		}
		return 0;
	}

	private Map<String, Object> parseWechatSuccessMap(String body) {
		final JsonNode root;
		try {
			root = objectMapper.readTree(body);
		} catch (IOException e) {
			throw new ResourceException("微信接口调用失败");
		}
		if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
			String msg = root.path("errmsg").asText("微信接口错误");
			if (!StringUtils.hasText(msg)) {
				msg = "微信接口错误";
			}
			throw new ResourceException(msg);
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
			Map.Entry<String, JsonNode> e = it.next();
			String key = e.getKey();
			if ("errcode".equals(key) || "errmsg".equals(key)) {
				continue;
			}
			out.put(key, objectMapper.convertValue(e.getValue(), Object.class));
		}
		return out;
	}

	private static String resolveWechatFailureMessage(Throwable e) {
		if (e == null) {
			return "微信接口调用失败";
		}
		String m = e.getMessage();
		return StringUtils.hasText(m) ? m : "微信接口调用失败";
	}
}
