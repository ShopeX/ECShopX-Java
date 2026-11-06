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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** ECX-10949：读取第一张表时按数据行数停止，并在结束后恢复解压条目上限。 */
class EspierUploadExcelSheetReaderLimitTest {

	@Test
	void moreThanMaxDataRows_throwsAndRestoresEntrySize() throws Exception {
		long before = ZipSecureFile.getMaxEntrySize();
		byte[] xlsx = buildXlsx(5001);

		assertThatThrownBy(() -> EspierUploadExcelSheetReader.readFirstSheet(xlsx, 5000))
				.isInstanceOf(EspierUploadExcelSheetReader.SheetRowLimitException.class)
				.hasMessageContaining("5000");
		assertThat(ZipSecureFile.getMaxEntrySize()).isEqualTo(before);
	}

	@Test
	void exactlyMaxDataRows_returnsHeaderAndValues() throws Exception {
		long before = ZipSecureFile.getMaxEntrySize();
		byte[] xlsx = buildXlsx(2);

		List<List<Object>> rows = EspierUploadExcelSheetReader.readFirstSheet(xlsx, 2);

		assertThat(rows).hasSize(3);
		assertThat(rows.get(0)).containsExactly("SKU");
		assertThat(rows.get(1)).containsExactly("v0");
		assertThat(rows.get(2)).containsExactly("v1");
		assertThat(ZipSecureFile.getMaxEntrySize()).isEqualTo(before);
		assertThat(EspierUploadExcelSheetReader.MAX_DECOMPRESSED_ENTRY_BYTES).isEqualTo(50L * 1024 * 1024);
	}

	private static byte[] buildXlsx(int dataRows) throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			var sheet = wb.createSheet();
			var header = sheet.createRow(0);
			header.createCell(0).setCellValue("SKU");
			for (int i = 0; i < dataRows; i++) {
				var row = sheet.createRow(i + 1);
				row.createCell(0).setCellValue("v" + i);
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			wb.write(bos);
			return bos.toByteArray();
		}
	}
}
