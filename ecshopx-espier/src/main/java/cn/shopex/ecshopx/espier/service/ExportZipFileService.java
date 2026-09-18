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

package cn.shopex.ecshopx.espier.service;

import cn.shopex.ecshopx.espier.storage.FileStorageService;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExportZipFileService {

	private static final Logger log = LoggerFactory.getLogger(ExportZipFileService.class);

	private final FileStorageService fileStorageService;

	public ExportZipFileService(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	/**
	 * 将目录树打成 zip 并上传至对象存储；返回 filedir、filename、url（与 CSV 导出语义一致）。
	 *
	 * @param directoryRoot 待打包的根目录（其下相对路径保留为 zip 内路径）
	 * @param fileBaseName  不含扩展名
	 */
	public Map<String, String> uploadZipFromDirectory(Path directoryRoot, String fileBaseName) {
		if (!StringUtils.hasText(fileBaseName) || directoryRoot == null || !Files.isDirectory(directoryRoot)) {
			log.debug("队列导出: 执行导出时失败");
			return Map.of();
		}
		String objectKey = "export/zip/" + fileBaseName + ".zip";
		String filename = fileBaseName + ".zip";
		Path tempZip = null;
		try {
			tempZip = Files.createTempFile("items-code-export-", ".zip");
			zipDirectory(directoryRoot, tempZip);
			byte[] content = Files.readAllBytes(tempZip);
			fileStorageService.put("file", objectKey, content);
			String url = fileStorageService.privateDownloadUrl("file", objectKey, 3600);
			Map<String, String> out = new LinkedHashMap<>();
			out.put("filedir", objectKey);
			out.put("filename", filename);
			out.put("url", url);
			return out;
		} catch (Exception e) {
			log.debug("队列导出: 执行导出时失败", e);
			return Map.of();
		} finally {
			if (tempZip != null) {
				try {
					Files.deleteIfExists(tempZip);
				} catch (IOException ignored) {
				}
			}
		}
	}

	private static void zipDirectory(Path sourceDir, Path zipFile) throws IOException {
		try (OutputStream fos = Files.newOutputStream(zipFile);
				ZipOutputStream zos = new ZipOutputStream(fos)) {
			Path base = sourceDir.toAbsolutePath().normalize();
			Files.walkFileTree(base, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Path rel = base.relativize(file.toAbsolutePath().normalize());
					String entryName = rel.toString().replace('\\', '/');
					if (entryName.isEmpty()) {
						return FileVisitResult.CONTINUE;
					}
					zos.putNextEntry(new ZipEntry(entryName));
					Files.copy(file, zos);
					zos.closeEntry();
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}
}
