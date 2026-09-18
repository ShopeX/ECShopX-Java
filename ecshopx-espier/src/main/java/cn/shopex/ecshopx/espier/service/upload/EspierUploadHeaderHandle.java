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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds Excel column index → field key mapping and validates required header labels.
 */
public final class EspierUploadHeaderHandle {

	private EspierUploadHeaderHandle() {}

	public static Map<Integer, String> buildColumnMap(List<String> headerRow, UploadHeaderTitle title) {
		if (title.all().isEmpty()) {
			throw new BadRequestException("导入表头与模板不匹配");
		}
		List<String> normalized = headerRow.stream().map(EspierUploadHeaderHandle::normalizeHeaderCell).toList();
		Set<String> hdrVals = new HashSet<>();
		for (String s : normalized) {
			if (s != null && !s.isEmpty()) {
				hdrVals.add(s);
			}
		}
		for (String requiredLabel : title.isNeed().keySet()) {
			if (!hdrVals.contains(normalizeHeaderCell(requiredLabel))) {
				throw new BadRequestException(requiredLabel + "必须导入");
			}
		}
		LinkedHashMap<Integer, String> column = new LinkedHashMap<>();
		for (int key = 0; key < normalized.size(); key++) {
			String columnName = normalized.get(key);
			if (columnName == null || columnName.isEmpty()) {
				continue;
			}
			for (Map.Entry<String, String> e : title.all().entrySet()) {
				if (normalizeHeaderCell(e.getKey()).equals(columnName)) {
					column.put(key, e.getValue());
					break;
				}
			}
		}
		if (column.isEmpty()) {
			throw new BadRequestException("导入表头与模板不匹配");
		}
		return column;
	}

	public static String normalizeHeaderCell(String raw) {
		if (raw == null) {
			return "";
		}
		return stripLeadingUtf8Bom(raw).replaceAll("[\\s　]", "");
	}

	/** Strip UTF-8 BOM that Excel keeps on the first export-CSV header cell. */
	private static String stripLeadingUtf8Bom(String raw) {
		if (raw == null || raw.isEmpty()) {
			return "";
		}
		String s = raw;
		if (s.startsWith("\uFEFF")) {
			s = s.substring(1);
		}
		if (s.startsWith("\u00EF\u00BB\u00BF")) {
			s = s.substring(3);
		}
		return s;
	}

	public static Map<String, Object> preRowHandle(Map<Integer, String> column, List<Object> rowCells) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		for (Map.Entry<Integer, String> e : column.entrySet()) {
			int idx = e.getKey();
			String col = e.getValue();
			if (idx >= 0 && idx < rowCells.size()) {
				Object v = rowCells.get(idx);
				if (v != null && v instanceof String s) {
					data.put(col, s.trim());
				} else {
					data.put(col, v == null ? null : String.valueOf(v).trim());
				}
			} else {
				data.put(col, null);
			}
		}
		return data;
	}

	public static boolean rowHasAnyValue(List<Object> rowCells) {
		if (rowCells == null) {
			return false;
		}
		for (Object o : rowCells) {
			if (o == null) {
				continue;
			}
			if (o instanceof String s) {
				if (!s.trim().isEmpty()) {
					return true;
				}
			} else if (o instanceof Number n) {
				if (n.doubleValue() != 0) {
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}
}
