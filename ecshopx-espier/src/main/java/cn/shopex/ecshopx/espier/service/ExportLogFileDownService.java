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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.domain.ExportLog;
import cn.shopex.ecshopx.espier.mapper.ExportLogMapper;
import cn.shopex.ecshopx.espier.storage.StorageProperties;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class ExportLogFileDownService {

	private final ExportLogMapper exportLogMapper;
	private final StorageProperties storageProperties;
	private final RestTemplate exportLogDownloadRestTemplate;

	public ExportLogFileDownService(
			ExportLogMapper exportLogMapper,
			StorageProperties storageProperties,
			@Qualifier("espierExportLogDownloadRestTemplate") RestTemplate exportLogDownloadRestTemplate) {
		this.exportLogMapper = exportLogMapper;
		this.storageProperties = storageProperties;
		this.exportLogDownloadRestTemplate = exportLogDownloadRestTemplate;
	}

	public ExportLogFileDownResult fileDown(String logIdRaw) {
		if (logIdRaw == null || logIdRaw.isBlank()) {
			throw new BadRequestException("缺少文件资源id");
		}
		long parsedLogId;
		try {
			parsedLogId = Long.parseLong(logIdRaw.trim());
		} catch (NumberFormatException ex) {
			throw new ResourceException("资源文件不存在");
		}

		ExportLog row = exportLogMapper.selectById(parsedLogId);
		if (row == null) {
			throw new ResourceException("资源文件不存在");
		}

		String fileUrl = row.getFileUrl();
		if (fileUrl == null || fileUrl.isBlank()) {
			throw new ResourceException("文件下载错误，请重新导出");
		}

		String trimmed = fileUrl.trim();
		URI uri;
		try {
			uri = URI.create(trimmed);
		} catch (IllegalArgumentException ex) {
			throw new ResourceException("文件下载错误，请重新导出");
		}

		String scheme = uri.getScheme();
		String host = uri.getHost();
		String csvData;
		if (scheme != null
				&& (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
				&& host != null
				&& !host.isBlank()) {
			try {
				byte[] bodyBytes = exportLogDownloadRestTemplate.getForObject(uri, byte[].class);
				if (bodyBytes == null) {
					throw new ResourceException("文件下载错误，请重新导出");
				}
				csvData = new String(bodyBytes, StandardCharsets.UTF_8);
			} catch (RestClientException ex) {
				throw new ResourceException("文件下载错误，请重新导出");
			}
		} else {
			String root = storageProperties.getLocal().getRoot();
			if (root == null || root.isBlank()) {
				throw new ResourceException("文件下载错误，请重新导出");
			}
			Path base = Paths.get(root).toAbsolutePath().normalize();
			Path target = base.resolve(trimmed).normalize();
			try {
				byte[] bytes = Files.readAllBytes(target);
				csvData = new String(bytes, StandardCharsets.UTF_8);
			} catch (IOException ex) {
				throw new ResourceException("文件下载错误，请重新导出");
			}
		}

		String exportType = row.getExportType() == null ? "" : row.getExportType();
		String fileName =
				DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
								.format(LocalDateTime.now(ZoneId.systemDefault()))
						+ exportType
						+ ".csv";

		return new ExportLogFileDownResult(csvData, fileName);
	}
}
