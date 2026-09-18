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

import cn.shopex.ecshopx.espier.storage.FileStorageService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class EspierUploadFileStorageWriter {

	private static final String DISK_KEY = "file";

	private final FileStorageService fileStorageService;

	public EspierUploadFileStorageWriter(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	public String writeImportFile(
			String requestFileType, long companyId, MultipartFile file, byte[] content, int uploadEpochSeconds) {
		String relative = buildRelativePathForImport(requestFileType, companyId, file, uploadEpochSeconds);
		fileStorageService.put(DISK_KEY, relative, content);
		return relative;
	}

	public String buildRelativePathForImport(
			String requestFileType, long companyId, MultipartFile file, int uploadEpochSeconds) {
		String fileName = file.getOriginalFilename();
		if (fileName == null || fileName.isBlank()) {
			fileName = "upload.xlsx";
		}
		return buildRelativePathForImport(requestFileType, companyId, fileName, uploadEpochSeconds);
	}

	/**
	 * 与 {@link #writeImportFile} 使用相同相对路径规则，供仅掌握元数据（无 MultipartFile）的定时任务删除侧复用。
	 */
	public String buildRelativePathForImport(
			String fileType, long companyId, String fileName, int uploadEpochSeconds) {
		String fn = fileName == null || fileName.isBlank() ? "upload.xlsx" : fileName;
		String fileExt = fileExt(fn);
		String md5Name = md5Hex(fn.getBytes(StandardCharsets.UTF_8));
		return fileType + "/" + companyId + "/" + uploadEpochSeconds + "/" + md5Name + fileExt;
	}

	private static String fileExt(String fileName) {
		int p = fileName.lastIndexOf('.');
		if (p < 0) {
			return ".xlsx";
		}
		String ext = fileName.substring(p);
		return ext.isEmpty() ? ".xlsx" : ext;
	}

	private static String md5Hex(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(data);
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
