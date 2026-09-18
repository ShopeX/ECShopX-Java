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

import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalRetFileAccessPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 与 {@link ChinaumsDivisionLocalStorageWriterService} 共用 storage 根；日终回盘只读访问。
 */
@Service
@Profile("!test-cron")
public class ChinaumsDivisionLocalRetFileAccessService implements ChinaumsDivisionLocalRetFileAccessPort {

	private final Path storageRoot;

	public ChinaumsDivisionLocalRetFileAccessService(
			@Value("${ecshopx.storage.local.root:storage/app/public}") String storageLocalRoot) {
		this.storageRoot = Path.of(storageLocalRoot).toAbsolutePath().normalize();
	}

	@Override
	public void ensureStorageDirectoryForRelativePath(String fileOrDirRelative) throws IOException {
		if (!StringUtils.hasText(fileOrDirRelative)) {
			return;
		}
		Files.createDirectories(resolve(fileOrDirRelative));
	}

	@Override
	public boolean exists(String relativeFilePath) throws IOException {
		if (!StringUtils.hasText(relativeFilePath)) {
			return false;
		}
		return Files.isRegularFile(resolve(relativeFilePath));
	}

	@Override
	public String readStringUtf8(String relativeFilePath) throws IOException {
		return Files.readString(resolve(relativeFilePath), StandardCharsets.UTF_8);
	}

	private Path resolve(String relativePathFromStorageRoot) {
		if (!StringUtils.hasText(relativePathFromStorageRoot)) {
			throw new IllegalStateException("invalid path");
		}
		String norm = relativePathFromStorageRoot.replace("\\", "/");
		if (norm.startsWith("/")) {
			norm = norm.substring(1);
		}
		Path t = storageRoot.resolve(norm).normalize();
		if (!t.startsWith(storageRoot)) {
			throw new IllegalStateException("invalid path under storage");
		}
		return t;
	}
}
