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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import me.chanjar.weixin.common.bean.CommonUploadParam;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.open.api.WxOpenMaPrivacyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WxaUploadPrivacyExtFileService {

	private final ObjectMapper objectMapper;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxJavaMaRuntime wxJavaMaRuntime;

	public WxaUploadPrivacyExtFileService(
			ObjectMapper objectMapper,
			WechatAuthQueryService wechatAuthQueryService,
			WxJavaMaRuntime wxJavaMaRuntime) {
		this.objectMapper = objectMapper;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxJavaMaRuntime = wxJavaMaRuntime;
	}

	public Map<String, Object> uploadPrivacyExtFile(long companyId, String wxaAppId, MultipartFile file) {
		if (companyId > 0 && !wechatAuthQueryService.isMiniProgramWxaBoundToCompany(companyId, wxaAppId)) {
			throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
		}

		if (file == null || file.isEmpty()) {
			throw new BadRequestException("上传文件失败");
		}
		String orig = file.getOriginalFilename();
		String ext = StringUtils.getFilenameExtension(orig);
		ext = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
		if (!"txt".equals(ext)) {
			throw new BadRequestException("仅支持txt");
		}
		long size = file.getSize();
		if (size <= 0) {
			throw new BadRequestException("上传文件失败");
		}
		if (size > 100L * 1024L) {
			throw new BadRequestException("大小不超过100kb");
		}

		Path dir = null;
		try {
			dir = Files.createTempDirectory("wxa-privacy-ext-");
			Path target = Files.createTempFile(dir, "upload-", ".txt");
			try (InputStream in = file.getInputStream()) {
				Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			}

			String body;
			try {
				body =
						wxJavaMaRuntime
								.ma(wxaAppId.trim())
								.upload(
										WxOpenMaPrivacyService.OPEN_UPLOAD_PRIVACY_FILE,
										CommonUploadParam.fromFile("file", target.toFile()));
			} catch (WxErrorException e) {
				throw new BadRequestException("微信接口请求失败");
			}

			JsonNode root;
			try {
				root = objectMapper.readTree(body == null ? "" : body);
			} catch (Exception e) {
				throw new BadRequestException("微信接口请求失败");
			}

			return jsonObjectToMap(root);
		} catch (BadRequestException | ResourceException e) {
			throw e;
		} catch (IOException e) {
			throw new BadRequestException("上传文件失败");
		} catch (Exception e) {
			throw new BadRequestException("微信接口请求失败");
		} finally {
			deleteDirectoryRecursively(dir);
		}
	}

	private Map<String, Object> jsonObjectToMap(JsonNode root) {
		if (root == null || !root.isObject()) {
			throw new BadRequestException("微信接口请求失败");
		}
		Map<String, Object> out = new LinkedHashMap<>();
		root.fields().forEachRemaining(e ->
				out.put(e.getKey(), objectMapper.convertValue(e.getValue(), Object.class)));
		return out;
	}

	private static void deleteDirectoryRecursively(Path dir) {
		if (dir == null || !Files.exists(dir)) {
			return;
		}
		try (Stream<Path> walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				} catch (IOException ignored) {
				}
			});
		} catch (IOException ignored) {
		}
	}
}
