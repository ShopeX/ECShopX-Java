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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

public abstract class AbstractEspierUploadFileHandler implements EspierUploadFileHandler {

	protected static final String XLSX = "xlsx";

	private MultipartProperties multipartProperties;

	@Autowired(required = false)
	public void setMultipartProperties(MultipartProperties multipartProperties) {
		this.multipartProperties = multipartProperties;
	}

	@Override
	public void check(MultipartFile file) {
		assertBaseUploadConstraints(file);
		checkDefaultExtension(file);
	}

	protected void checkDefaultExtension(MultipartFile file) {
		String ext = extensionOf(file);
		if (!XLSX.equalsIgnoreCase(ext)) {
			String joined = Arrays.stream(defaultExtensions())
					.map(s -> s.toLowerCase())
					.collect(Collectors.joining("、"));
			throw new BadRequestException(String.format("上传文件只支持%s格式", joined));
		}
	}

	/**
	 * Rejects null parts and single files larger than {@code spring.servlet.multipart.max-file-size} when configured.
	 */
	protected void assertBaseUploadConstraints(MultipartFile file) {
		if (file == null) {
			throw new BadRequestException("上传文件内容必填");
		}
		if (multipartProperties != null) {
			DataSize maxFile = multipartProperties.getMaxFileSize();
			if (maxFile != null && maxFile.toBytes() > 0 && file.getSize() > maxFile.toBytes()) {
				throw new BadRequestException("上传文件大小超出限制");
			}
		}
	}

	protected String[] defaultExtensions() {
		return new String[] {XLSX};
	}

	public static String extensionOf(MultipartFile file) {
		String name = file.getOriginalFilename();
		if (name == null || !name.contains(".")) {
			return "";
		}
		int d = name.lastIndexOf('.');
		return name.substring(d + 1);
	}
}
