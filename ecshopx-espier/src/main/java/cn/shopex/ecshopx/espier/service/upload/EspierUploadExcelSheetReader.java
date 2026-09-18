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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public final class EspierUploadExcelSheetReader {

	private static final DataFormatter FMT = new DataFormatter();

	private EspierUploadExcelSheetReader() {}

	public static List<List<Object>> readFirstSheet(byte[] xlsxBytes) throws IOException {
		try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xlsxBytes))) {
			Sheet sheet = wb.getSheetAt(0);
			List<List<Object>> rows = new ArrayList<>();
			int last = sheet.getLastRowNum();
			for (int r = 0; r <= last; r++) {
				Row row = sheet.getRow(r);
				List<Object> cells = new ArrayList<>();
				if (row == null) {
					rows.add(cells);
					continue;
				}
				short lastCell = row.getLastCellNum();
				for (int c = 0; c < lastCell; c++) {
					Cell cell = row.getCell(c);
					if (cell == null) {
						cells.add("");
						continue;
					}
					cells.add(FMT.formatCellValue(cell));
				}
				rows.add(cells);
			}
			return rows;
		}
	}
}
