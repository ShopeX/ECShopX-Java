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

package cn.shopex.ecshopx.goods.service.hotword;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 热词图片上传（ECX-11390 F-002/AC-004、AC-005）。
 *
 * <p>通用上传（{@code /api/v1/espier/upload_file}）只允许 jpg/jpeg/png 且上限 50 MB，无法满足热词
 * 「gif/png/jpg + 小于 5 MB」的要求，因此热词图片走本服务：先校验格式、大小与文件头，再写入 {@code image}
 * 存储盘，键形如 {@code hotword/{companyId}/{yyyy}/{MM}/{dd}/{uuid}.{ext}}。
 */
@Service
public class SearchHotwordImageUploadService {

	private static final Logger log = LoggerFactory.getLogger(SearchHotwordImageUploadService.class);

	private static final String STORAGE_FILE_TYPE = "image";

	private static final String GROUP = "hotword";

	private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

	private final FileStorageService fileStorageService;

	public SearchHotwordImageUploadService(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	/**
	 * 校验并保存图片，返回可直接写入 {@code image_url} 的地址。
	 */
	public Map<String, Object> upload(long companyId, MultipartFile file) {
		if (file == null) {
			throw new BadRequestException("请上传图片文件", Map.of("file", List.of("validation.required")), 422);
		}
		String ext = SearchHotwordImageRules.requireAllowedExtension(file.getOriginalFilename());
		if (file.getSize() >= SearchHotwordImageRules.MAX_BYTES) {
			SearchHotwordImageRules.assertSize(file.getSize());
		}

		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException e) {
			log.warn("热词图片读取失败", e);
			throw new ResourceException("上传失败");
		}
		SearchHotwordImageRules.assertSize(bytes.length);
		SearchHotwordImageRules.assertContentMatches(bytes, ext);

		String key = buildObjectKey(companyId, ext);
		try {
			fileStorageService.put(STORAGE_FILE_TYPE, key, bytes);
		} catch (RuntimeException e) {
			log.warn("热词图片写入存储失败: {}", key, e);
			throw new ResourceException("上传失败");
		}
		String url = fileStorageService.url(STORAGE_FILE_TYPE, key);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("url", url);
		result.put("size", bytes.length);
		result.put("ext", ext);
		return result;
	}

	private static String buildObjectKey(long companyId, String ext) {
		String datePath = DATE_PATH.format(ZonedDateTime.now(SearchHotwordTime.ZONE));
		String uuid = UUID.randomUUID().toString().replace("-", "");
		return GROUP + "/" + companyId + "/" + datePath + "/" + uuid + "." + ext;
	}
}
