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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class EspierUploadTemplateXlsxBuilder {

	private static final Pattern SHEET_NAME_INVALID = Pattern.compile("[\\\\/*?:\\[\\]]");

	private EspierUploadTemplateXlsxBuilder() {}

	public static byte[] buildTemplateBytes(
			String mainSheetName,
			UploadHeaderTitle headerTitle,
			Optional<List<List<Object>>> demoRows,
			Optional<List<String>> textColumnHeaders) {
		try (XSSFWorkbook workbook = new XSSFWorkbook();
				ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			String sanitized = sanitizeSheetName(mainSheetName);
			String sheetTitle = sanitized.isEmpty() ? "Sheet1" : sanitized;
			Sheet main = workbook.createSheet(sheetTitle);

			List<String> headerLabels = new ArrayList<>(headerTitle.all().keySet());
			Row headerRow = main.createRow(0);
			for (int c = 0; c < headerLabels.size(); c++) {
				headerRow.createCell(c).setCellValue(headerLabels.get(c));
			}

			if (demoRows.isPresent() && !demoRows.get().isEmpty()) {
				int rowIndex = 1;
				for (List<Object> cells : demoRows.get()) {
					Row row = main.createRow(rowIndex++);
					if (cells == null) {
						continue;
					}
					for (int i = 0; i < cells.size(); i++) {
						Object v = cells.get(i);
						row.createCell(i).setCellValue(v == null ? "" : String.valueOf(v));
					}
				}
			}

			if (textColumnHeaders.isPresent() && !textColumnHeaders.get().isEmpty()) {
				DataFormat dataFormat = workbook.createDataFormat();
				CellStyle textStyle = workbook.createCellStyle();
				textStyle.setDataFormat(dataFormat.getFormat("@"));
				for (String colName : textColumnHeaders.get()) {
					int idx = headerLabels.indexOf(colName);
					if (idx >= 0) {
						main.setDefaultColumnStyle(idx, textStyle);
					}
				}
			}

			Map<String, UploadHeaderColumnInfo> headerInfo = headerTitle.headerInfo();
			if (headerInfo != null && !headerInfo.isEmpty()) {
				Sheet info = workbook.createSheet("填写说明");
				Row titleRow = info.createRow(0);
				titleRow.createCell(0).setCellValue("名称");
				titleRow.createCell(1).setCellValue("最大长度");
				titleRow.createCell(2).setCellValue("是否必填");
				titleRow.createCell(3).setCellValue("备注");
				int rowIndex = 1;
				for (Map.Entry<String, UploadHeaderColumnInfo> e : headerInfo.entrySet()) {
					UploadHeaderColumnInfo col = e.getValue();
					Row row = info.createRow(rowIndex++);
					row.createCell(0).setCellValue(e.getKey());
					row.createCell(1).setCellValue(col.size() + "位");
					row.createCell(2).setCellValue(col.isNeed() ? "是" : "否");
					String remarks = col.remarks();
					row.createCell(3).setCellValue(remarks == null ? "" : remarks);
				}
			}

			workbook.write(baos);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String sanitizeSheetName(String raw) {
		if (raw == null) {
			return "";
		}
		String s = SHEET_NAME_INVALID.matcher(raw).replaceAll("_");
		if (s.length() > 31) {
			return s.substring(0, 31);
		}
		return s;
	}
}
