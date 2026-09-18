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

package cn.shopex.ecshopx.espier.openapi;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiImageUploadTokenPort;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OpenapiImageUploadTokenPortImpl implements OpenapiImageUploadTokenPort {

	private final FileStorageService fileStorageService;

	public OpenapiImageUploadTokenPortImpl(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	@Override
	public Map<String, Object> getUploadToken(String fileType, long companyId, String group, String filename) {
		return fileStorageService.getUploadToken(fileType, companyId, group, filename);
	}

	@Override
	public String uploadLocalImage(String fileType, long companyId, String group, MultipartFile file) {
		Map<String, Object> result = fileStorageService.uploadeImage(fileType, companyId, group, file);
		Object tokenObj = result.get("token");
		if (!(tokenObj instanceof Map<?, ?> token)) {
			throw new ResourceException("上传失败");
		}
		Object keyObj = token.get("key");
		if (!(keyObj instanceof String imageUrl) || imageUrl.isBlank()) {
			throw new ResourceException("上传失败");
		}
		return imageUrl;
	}
}
