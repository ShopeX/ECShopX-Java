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

package cn.shopex.ecshopx.espier.service.upgrade;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UpgradeRollbackService {

	private static final Logger log = LoggerFactory.getLogger(UpgradeRollbackService.class);

	private final String storageRoot;

	public UpgradeRollbackService(@Value("${espier.upgrade.storage-root:}") String storageRoot) {
		this.storageRoot = storageRoot;
	}

	public void rollback() {
		if (storageRoot == null || storageRoot.isBlank()) {
			log.warn("espier.upgrade.storage-root 未配置，跳过回滚文件操作");
			return;
		}
		Path storageRootPath = Paths.get(storageRoot.trim()).toAbsolutePath().normalize();
		unlinkBakUpgradeFile(storageRootPath);
		unlinkNewUpgradeFile(storageRootPath);
	}

	private void unlinkBakUpgradeFile(Path storageRootPath) {
		Path oldList = storageRootPath.resolve("upgrade").resolve("oldfiles.txt");
		if (!Files.isRegularFile(oldList)) {
			return;
		}
		List<String> lines;
		try {
			lines = Files.readAllLines(oldList, StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.warn("读取回滚清单失败: {}", oldList, e);
			return;
		}
		for (String rawLine : lines) {
			String line = rawLine.replace("\u0000", "").trim();
			if (line.isEmpty()) {
				continue;
			}
			try {
				Path bakPath = Paths.get(line).normalize();
				String p = bakPath.toString();
				Path targetPath;
				if (p.endsWith(".bak")) {
					String targetStr = p.substring(0, p.length() - 4);
					targetPath = Paths.get(targetStr);
				} else {
					targetPath = bakPath;
				}
				if (!Files.isRegularFile(bakPath)) {
					log.warn("备份文件不存在或不是常规文件，跳过: {}", bakPath);
					continue;
				}
				byte[] bytes = Files.readAllBytes(bakPath);
				Path parent = targetPath.getParent();
				if (parent != null && !Files.exists(parent)) {
					Files.createDirectories(parent);
				}
				Files.write(targetPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				if (Files.isRegularFile(bakPath)) {
					Files.delete(bakPath);
				}
			} catch (IOException e) {
				log.warn("处理 oldfiles 行失败: {}", rawLine, e);
			}
		}
	}

	private void unlinkNewUpgradeFile(Path storageRootPath) {
		Path list = storageRootPath.resolve("upgrade").resolve("newfiles.txt");
		if (!Files.isRegularFile(list)) {
			return;
		}
		List<String> lines;
		try {
			lines = Files.readAllLines(list, StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.warn("读取新增文件清单失败: {}", list, e);
			return;
		}
		for (String rawLine : lines) {
			String line = rawLine.trim();
			if (line.isEmpty()) {
				continue;
			}
			Path file = Paths.get(line).normalize();
			delFileAndEmptyDir(file);
		}
	}

	private void delFileAndEmptyDir(Path file) {
		try {
			if (Files.isRegularFile(file)) {
				Files.delete(file);
			}
		} catch (IOException e) {
			log.warn("删除文件失败: {}", file, e);
			return;
		}
		Path dir = file.getParent();
		if (dir == null) {
			return;
		}
		try {
			Files.delete(dir);
		} catch (IOException e) {
			log.warn("删除空目录失败: {}", dir, e);
			return;
		}
		delEmptyDirParents(dir.getParent());
	}

	private void delEmptyDirParents(Path parent) {
		if (parent == null) {
			return;
		}
		try {
			Files.delete(parent);
			delEmptyDirParents(parent.getParent());
		} catch (IOException e) {
			// 非空或不存在时停止向上清理
			log.warn("向上清理空目录失败，停止递归: {}", parent, e);
		}
	}
}
