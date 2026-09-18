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
import cn.shopex.ecshopx.common.export.ItemsExportCsvPollingPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class ExportCsvDataService {

	private static final Logger log = LoggerFactory.getLogger(ExportCsvDataService.class);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper exportKeyObjectMapper;
	private final ItemsExportCsvPollingPort itemsExportCsvPollingPort;

	public ExportCsvDataService(@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redisTemplate,
			@Qualifier("exportKeyFilterObjectMapper") ObjectMapper exportKeyObjectMapper,
			ItemsExportCsvPollingPort itemsExportCsvPollingPort) {
		this.redisTemplate = redisTemplate;
		this.exportKeyObjectMapper = exportKeyObjectMapper;
		this.itemsExportCsvPollingPort = itemsExportCsvPollingPort;
	}

	public ExportCsvDataResult exportCsvData(String pageRaw, String keyRaw, String totalCountRaw) {
		String key = (keyRaw == null || keyRaw.isEmpty()) ? "0" : keyRaw;
		String redisKey = "export_key:" + key;
		String raw = redisTemplate.opsForValue().get(redisKey);
		if (raw == null || raw.isBlank()) {
			return ExportCsvDataResult.keyError();
		}
		Map<String, Object> filter = parseFilterObject(raw);
		Object exportTypeObj = filter.remove("export_type");
		String exportType = "items";
		if (exportTypeObj != null && !exportTypeObj.toString().isBlank()) {
			exportType = exportTypeObj.toString().trim().toLowerCase();
		}
		if (!"items".equals(exportType)) {
			throw new BadRequestException("无此导出类型");
		}
		String fileName = itemsExportCsvPollingPort.getFileName(filter);
		List<String> titleHeaders = itemsExportCsvPollingPort.getTitleRow(filter);
		int page = parseExportPageFromQuery(pageRaw);
		List<List<String>> res = new ArrayList<>();
		if (page == 1) {
			res.add(new ArrayList<>(titleHeaders));
		}
		filter.remove("is_default");
		filter.remove("operator_type");
		filter.remove("item_source");
		int progressPageSize = 100;
		log.info("exportCsvData:totalCount:{}", totalCountRaw);
		int divisor;
		Object totalCountPayload;
		if (totalCountRawMustFetchCount(totalCountRaw)) {
			int c = itemsExportCsvPollingPort.getCount(filter);
			divisor = c;
			totalCountPayload = c;
			log.info("exportCsvData:totalCount:{}", c);
		} else {
			divisor = parseExportPageFromQuery(totalCountRaw);
			totalCountPayload = totalCountRaw;
		}
		filter.remove("isGetSkuList");
		List<LinkedHashMap<String, String>> dataList =
				itemsExportCsvPollingPort.getListsApiReturn(filter, page, progressPageSize);
		int percentDisplay;
		if (divisor <= 0) {
			percentDisplay = 100;
		} else {
			int percentRaw = (int) Math.ceil((double) page * progressPageSize * 100 / divisor);
			percentDisplay = Math.min(percentRaw, 100);
		}
		if (CollectionUtils.isEmpty(dataList)) {
			redisTemplate.delete(redisKey);
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("data", List.of());
			payload.put("percent", 100);
			payload.put("total_count", totalCountPayload);
			payload.put("file_name", fileName);
			return ExportCsvDataResult.ok(payload);
		}
		for (LinkedHashMap<String, String> row : dataList) {
			res.add(new ArrayList<>(row.values()));
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("data", res);
		payload.put("percent", percentDisplay);
		payload.put("total_count", totalCountPayload);
		payload.put("file_name", fileName);
		return ExportCsvDataResult.ok(payload);
	}

	private static boolean totalCountRawMustFetchCount(String totalCountRaw) {
		if (totalCountRaw == null) {
			return true;
		}
		if (totalCountRaw.isEmpty()) {
			return true;
		}
		return "0".equals(totalCountRaw);
	}

	private Map<String, Object> parseFilterObject(String raw) {
		JsonNode root;
		try {
			root = exportKeyObjectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("无效导出参数");
		}
		if (root == null || !root.isObject()) {
			throw new BadRequestException("无效导出参数");
		}
		try {
			return exportKeyObjectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("无效导出参数");
		}
	}

	/**
	 * 将导出接口 Query 中的数字串规范为 int：跳过前导 ASCII 空白；可选单个正负号；读取最长十进制数字前缀；
	 * 无数字则为 0；绝对值按 long 解析后饱和到 int。
	 */
	private static int parseExportPageFromQuery(String s) {
		if (s == null) {
			return 0;
		}
		int i = 0;
		int n = s.length();
		while (i < n && isAsciiWhitespace(s.charAt(i))) {
			i++;
		}
		if (i >= n) {
			return 0;
		}
		int sign = 1;
		char c0 = s.charAt(i);
		if (c0 == '+' || c0 == '-') {
			sign = c0 == '-' ? -1 : 1;
			i++;
			if (i >= n) {
				return 0;
			}
			char c1 = s.charAt(i);
			if (c1 == '+' || c1 == '-') {
				return 0;
			}
		}
		int startDigits = i;
		while (i < n && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
			i++;
		}
		if (i == startDigits) {
			return 0;
		}
		String absDigits = s.substring(startDigits, i);
		long absVal;
		try {
			absVal = Long.parseLong(absDigits);
		} catch (NumberFormatException e) {
			absVal = Long.MAX_VALUE;
		}
		long signed = sign * absVal;
		if (signed > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (signed < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) signed;
	}

	private static boolean isAsciiWhitespace(char c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == 0x0B;
	}
}
