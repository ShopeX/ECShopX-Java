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
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;

/**
 * 上传公众号素材至微信服务器。成功响应中 {@code media_id}、{@code url} 为常见顶层字段；部分素材类型可能仅有
 * {@code media_id}，以微信开放平台返回为准。
 */
@Service
public class WechatOfficialAccountMaterialUploadService {

	private static String wxMpUrl(me.chanjar.weixin.mp.enums.WxMpApiUrl u) {
		return u.getPrefix() + u.getPath();
	}

	private final RestTemplate restTemplate = new RestTemplate();

	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final ObjectMapper objectMapper;

	public WechatOfficialAccountMaterialUploadService(
			WechatOpenPlatformAuthorizerTokenService tokenService, ObjectMapper objectMapper) {
		this.tokenService = tokenService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> uploadMaterial(
			String authorizerAppid,
			boolean isTemporaryMaterial,
			String materialType,
			MultipartFile file,
			String title,
			String description) {
		if (isTemporaryMaterial && "video".equals(materialType)) {
			throw new BadRequestException(
					"微信公众平台「上传临时素材」接口仅接受 media 文件与 type 参数，不提供标题与简介字段；临时视频无法与永久素材 add_material 的 description JSON 对齐");
		}
		String token;
		try {
			token = tokenService.getAuthorizerAccessToken(authorizerAppid.trim());
		} catch (ResourceException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ResourceException(resolveWechatFailureMessage(e));
		}
		if (!StringUtils.hasText(token)) {
			throw new ResourceException("微信接口调用失败");
		}

		Path dir = null;
		try {
			dir = Files.createTempDirectory("wechat-material-");
			String safeName = sanitizeOriginalFilename(file.getOriginalFilename());
			Path target = dir.resolve(safeName);
			try (InputStream in = file.getInputStream()) {
				Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			}
			String url = buildUploadUrl(isTemporaryMaterial, materialType, token.trim());
			String responseBody = postMultipart(url, isTemporaryMaterial, materialType, target, title, description);
			return parseWechatSuccessMap(responseBody);
		} catch (IOException e) {
			throw new ResourceException("微信接口调用失败");
		} finally {
			deleteRecursively(dir);
		}
	}

	public Map<String, Object> uploadArticleImage(String authorizerAppid, MultipartFile file) {
		String token;
		try {
			token = tokenService.getAuthorizerAccessToken(authorizerAppid.trim());
		} catch (ResourceException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ResourceException(resolveWechatFailureMessage(e));
		}
		if (!StringUtils.hasText(token)) {
			throw new ResourceException("微信接口调用失败");
		}

		Path dir = null;
		try {
			dir = Files.createTempDirectory("wechat-article-img-");
			String safeName = sanitizeOriginalFilename(file.getOriginalFilename());
			Path mediaFile = dir.resolve(safeName);
			try (InputStream in = file.getInputStream()) {
				Files.copy(in, mediaFile, StandardCopyOption.REPLACE_EXISTING);
			}
			String url = UriComponentsBuilder.fromHttpUrl(wxMpUrl(WxMpApiUrl.Material.IMG_UPLOAD_URL))
					.queryParam("access_token", token.trim())
					.build(true)
					.toUriString();

			MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
			parts.add("media", new FileSystemResource(mediaFile.toFile()));
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(parts, headers);
			String body;
			try {
				ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
				body = resp.getBody();
			} catch (RestClientException e) {
				throw new ResourceException(resolveWechatFailureMessage(e));
			}
			if (!StringUtils.hasText(body)) {
				throw new ResourceException("微信接口调用失败");
			}
			return parseWechatSuccessMap(body);
		} catch (IOException e) {
			throw new ResourceException("微信接口调用失败");
		} finally {
			deleteRecursively(dir);
		}
	}

	public void deleteMaterial(String authorizerAppid, String mediaIdsRaw) {
		String token;
		try {
			token = tokenService.getAuthorizerAccessToken(authorizerAppid.trim());
		} catch (ResourceException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ResourceException(resolveWechatFailureMessage(e));
		}
		if (!StringUtils.hasText(token)) {
			throw new ResourceException("微信接口调用失败");
		}

		String[] segments = mediaIdsRaw.split(",", -1);
		for (String segment : segments) {
			String url =
					wxMpUrl(WxMpApiUrl.Material.MATERIAL_DEL_URL)
							+ "?access_token="
							+ URLEncoder.encode(token.trim(), StandardCharsets.UTF_8);
			LinkedHashMap<String, Object> req = new LinkedHashMap<>();
			req.put("media_id", segment);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<LinkedHashMap<String, Object>> entity = new HttpEntity<>(req, headers);
			String body;
			try {
				ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
				body = resp.getBody();
			} catch (RestClientException e) {
				throw new ResourceException(resolveWechatFailureMessage(e));
			}
			if (!StringUtils.hasText(body)) {
				throw new ResourceException("微信接口调用失败");
			}
			parseWechatSuccessMap(body);
		}
	}

	private String buildUploadUrl(boolean temporary, String materialType, String token) {
		if (temporary) {
			return UriComponentsBuilder.fromHttpUrl(wxMpUrl(WxMpApiUrl.Material.MEDIA_UPLOAD_URL))
					.queryParam("access_token", token)
					.queryParam("type", materialType)
					.build(true)
					.toUriString();
		}
		return UriComponentsBuilder.fromHttpUrl(wxMpUrl(WxMpApiUrl.Material.MATERIAL_ADD_URL))
				.queryParam("access_token", token)
				.queryParam("type", materialType)
				.build(true)
				.toUriString();
	}

	private String postMultipart(
			String url,
			boolean temporary,
			String materialType,
			Path mediaFile,
			String title,
			String description) {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("media", new FileSystemResource(mediaFile.toFile()));
		if (!temporary && "video".equals(materialType)) {
			String descriptionJson;
			try {
				Map<String, String> desc = new LinkedHashMap<>();
				desc.put("title", title == null ? "" : title);
				desc.put("introduction", description == null ? "" : description);
				descriptionJson = objectMapper.writeValueAsString(desc);
			} catch (IOException e) {
				throw new ResourceException("微信接口调用失败");
			}
			parts.add("description", descriptionJson);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(parts, headers);
		try {
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			String body = resp.getBody();
			if (!StringUtils.hasText(body)) {
				throw new ResourceException("微信接口调用失败");
			}
			return body;
		} catch (RestClientException e) {
			throw new ResourceException(resolveWechatFailureMessage(e));
		}
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

	private static String sanitizeOriginalFilename(String originalFilename) {
		if (!StringUtils.hasText(originalFilename)) {
			return "upload.bin";
		}
		String name = originalFilename.replace('\\', '/');
		int idx = name.lastIndexOf('/');
		if (idx >= 0) {
			name = name.substring(idx + 1);
		}
		if (!StringUtils.hasText(name)) {
			return "upload.bin";
		}
		return name;
	}

	private static void deleteRecursively(Path dir) {
		if (dir == null || !Files.exists(dir)) {
			return;
		}
		try {
			Files.walk(dir)
					.sorted(Comparator.reverseOrder())
					.forEach(p -> {
						try {
							Files.deleteIfExists(p);
						} catch (IOException ignored) {
						}
					});
		} catch (IOException ignored) {
		}
	}
}
