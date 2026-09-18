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

package cn.shopex.ecshopx.chinaumspay.transfer;

import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalArtifactWriterPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 将划付/分账文件写到 {@code ecshopx.storage.local.root} 下，与 PHP {@code storage_path} 对齐。
 */
@Service
@Profile("!test-cron")
public class ChinaumsDivisionLocalStorageWriterService implements ChinaumsDivisionLocalArtifactWriterPort {

	private final Path storageRoot;

	public ChinaumsDivisionLocalStorageWriterService(
			@Value("${ecshopx.storage.local.root:storage/app/public}") String storageLocalRoot) {
		this.storageRoot = Path.of(storageLocalRoot).toAbsolutePath().normalize();
	}

	@Override
	public void put(String relativePathFromStorageRoot, String fileContent) {
		if (!StringUtils.hasText(relativePathFromStorageRoot)) {
			return;
		}
		String norm = relativePathFromStorageRoot.replace("\\", "/");
		if (norm.startsWith("/")) {
			norm = norm.substring(1);
		}
		Path target = storageRoot.resolve(norm).normalize();
		if (!target.startsWith(storageRoot)) {
			throw new IllegalStateException("invalid path under storage");
		}
		try {
			Files.createDirectories(target.getParent());
			Files.writeString(target, fileContent == null ? "" : fileContent, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("write division artifact failed: " + target, e);
		}
	}
}
