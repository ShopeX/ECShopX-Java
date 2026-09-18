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

package cn.shopex.ecshopx.espier.service.upload;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 服务端解码图片字节后的落盘与可访问 URL 生成。
 * 已改为委托 {@link FileStorageService}，支持 local / oss 等多驱动。
 */
@Service
public class EspierImageUploadClient {

	private final FileStorageService fileStorageService;

	public EspierImageUploadClient(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	public String uploadDecodedImage(long companyId, byte[] imageBytes, String inferredMime) {
		if (imageBytes == null || imageBytes.length == 0) {
			throw new ResourceException("图片数据为空");
		}
		String ext = extensionForMime(inferredMime);
		String filename = java.util.UUID.randomUUID().toString().replace("-", "") + ext;
		Map<String, Object> result = fileStorageService.upload("image", companyId, null, filename, imageBytes);
		@SuppressWarnings("unchecked")
		Map<String, Object> token = (Map<String, Object>) result.get("token");
		String domain = (String) token.get("domain");
		String key = (String) token.get("key");
		if (domain == null || domain.isEmpty()) {
			return key;
		}
		String base = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
		return base + "/" + key;
	}

	private static String extensionForMime(String mime) {
		if (mime == null || mime.isEmpty()) {
			return ".bin";
		}
		return switch (mime.toLowerCase()) {
			case "image/png" -> ".png";
			case "image/jpeg", "image/jpg" -> ".jpg";
			case "image/gif" -> ".gif";
			case "image/webp" -> ".webp";
			default -> ".bin";
		};
	}
}
