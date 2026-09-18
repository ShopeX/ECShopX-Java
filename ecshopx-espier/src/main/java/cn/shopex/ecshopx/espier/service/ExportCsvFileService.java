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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExportCsvFileService {

	private static final Logger log = LoggerFactory.getLogger(ExportCsvFileService.class);

	private final FileStorageService fileStorageService;

	public ExportCsvFileService(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	/**
	 * 写 UTF-8 BOM CSV，上传至对象存储；返回 filedir、filename、url（与私有下载语义一致）。
	 *
	 * @param fileBaseName 不含扩展名
	 */
	public Map<String, String> exportCsv(String fileBaseName, LinkedHashMap<String, String> titleKeysToHeader,
			List<Map<String, String>> rows) {
		if (!StringUtils.hasText(fileBaseName)) {
			log.debug("队列导出: 执行导出时失败");
			return Map.of();
		}
		String objectKey = "export/csv/" + fileBaseName + ".csv";
		String filename = fileBaseName + ".csv";
		Path temp = null;
		try {
			temp = Files.createTempFile("epidemic-export-", ".csv");
			writeCsvFile(temp, titleKeysToHeader, rows);
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
			if (temp != null) {
				try {
					Files.deleteIfExists(temp);
				} catch (IOException ignored) {
				}
			}
		}
	}

	private void writeCsvFile(Path temp, LinkedHashMap<String, String> titleKeysToHeader,
			List<Map<String, String>> rows) throws IOException {
		try (BufferedWriter w = new BufferedWriter(
				new OutputStreamWriter(Files.newOutputStream(temp), StandardCharsets.UTF_8))) {
			w.write('\uFEFF');
			writeCsvLine(w, titleKeysToHeader.values());
			for (Map<String, String> row : rows) {
				List<String> cells = titleKeysToHeader.keySet().stream().map(k -> row.getOrDefault(k, "--")).toList();
				writeCsvLine(w, cells);
			}
		}
	}

	private static void writeCsvLine(BufferedWriter w, Iterable<String> cells) throws IOException {
		boolean first = true;
		for (String cell : cells) {
			if (!first) {
				w.write(',');
			}
			first = false;
			writeCsvField(w, cell != null ? cell : "");
		}
		w.write('\n');
	}

	private static void writeCsvField(BufferedWriter w, String s) throws IOException {
		boolean needQuote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\\') >= 0 || s.indexOf('\n') >= 0
				|| s.indexOf('\r') >= 0;
		if (!needQuote) {
			w.write(s);
			return;
		}
		w.write('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\') {
				w.write("\\\\");
			} else if (c == '"') {
				w.write("\\\"");
			} else {
				w.write(c);
			}
		}
		w.write('"');
	}
}
