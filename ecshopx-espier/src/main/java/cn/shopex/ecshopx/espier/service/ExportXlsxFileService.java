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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExportXlsxFileService {

	private static final Logger log = LoggerFactory.getLogger(ExportXlsxFileService.class);

	private final FileStorageService fileStorageService;

	public ExportXlsxFileService(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	/**
	 * 将工作簿写入临时 xlsx 并上传至 import-file 语义存储，返回 filedir、filename、url。
	 *
	 * @param fileBaseName 不含扩展名
	 */
	public Map<String, String> exportXlsx(String fileBaseName, Workbook workbook) {
		if (!StringUtils.hasText(fileBaseName) || workbook == null) {
			log.debug("队列导出: 执行导出时失败");
			return Map.of();
		}
		String objectKey = "export/xlsx/" + fileBaseName + ".xlsx";
		String filename = fileBaseName + ".xlsx";
		Path temp = null;
		try {
			temp = Files.createTempFile("export-xlsx-", ".xlsx");
			try (OutputStream os = Files.newOutputStream(temp)) {
				workbook.write(os);
			}
			byte[] content = Files.readAllBytes(temp);
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
			try {
				workbook.close();
			} catch (IOException ignored) {
			}
			if (temp != null) {
				try {
					Files.deleteIfExists(temp);
				} catch (IOException ignored) {
				}
			}
		}
	}
}
