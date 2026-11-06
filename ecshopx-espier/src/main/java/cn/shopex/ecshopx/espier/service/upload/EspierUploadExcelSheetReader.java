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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public final class EspierUploadExcelSheetReader {

	/** ECX-10949：本次读取允许的解压后单条目大小，50MB。 */
	public static final long MAX_DECOMPRESSED_ENTRY_BYTES = 50L * 1024 * 1024;

	private static final DataFormatter FMT = new DataFormatter();

	private EspierUploadExcelSheetReader() {}

	/**
	 * 读取第一张表。表头不计入 {@code maxDataRows}。超过上限时停止并抛出 {@link SheetRowLimitException}。
	 * 读取期间把解压后单条目上限收紧到 {@link #MAX_DECOMPRESSED_ENTRY_BYTES}，结束时恢复原值。
	 */
	public static List<List<Object>> readFirstSheet(byte[] xlsxBytes, int maxDataRows) throws IOException {
		if (maxDataRows < 0) {
			throw new IllegalArgumentException("maxDataRows");
		}
		long previousMaxEntrySize = ZipSecureFile.getMaxEntrySize();
		ZipSecureFile.setMaxEntrySize(MAX_DECOMPRESSED_ENTRY_BYTES);
		try {
			if (isZip(xlsxBytes)) {
				return readFirstSheetStreaming(xlsxBytes, maxDataRows);
			}
			return readFirstSheetWorkbook(xlsxBytes, maxDataRows);
		} finally {
			ZipSecureFile.setMaxEntrySize(previousMaxEntrySize);
		}
	}

	private static boolean isZip(byte[] bytes) {
		return bytes != null
				&& bytes.length >= 4
				&& bytes[0] == 'P'
				&& bytes[1] == 'K'
				&& bytes[2] == 3
				&& bytes[3] == 4;
	}

	private static List<List<Object>> readFirstSheetStreaming(byte[] xlsxBytes, int maxDataRows) throws IOException {
		try (OPCPackage pkg = OPCPackage.open(new ByteArrayInputStream(xlsxBytes))) {
			XSSFReader reader = new XSSFReader(pkg);
			SharedStrings strings = reader.getSharedStringsTable();
			StylesTable styles = reader.getStylesTable();
			Iterator<InputStream> sheets = reader.getSheetsData();
			if (!sheets.hasNext()) {
				throw new IOException("空表格");
			}
			try (InputStream sheet = sheets.next()) {
				SheetCollector collector = new SheetCollector(maxDataRows);
				XSSFSheetXMLHandler handler =
						new XSSFSheetXMLHandler(styles, strings, collector, new DataFormatter(), false);
				XMLReader parser = XMLHelper.newXMLReader();
				parser.setContentHandler(handler);
				try {
					parser.parse(new InputSource(sheet));
				} catch (SAXException | RuntimeException ex) {
					SheetRowLimitException limit = findRowLimit(ex);
					if (limit != null) {
						throw limit;
					}
					if (ex instanceof SAXException sax) {
						throw new IOException(sax.getMessage(), sax);
					}
					throw ex;
				}
				if (collector.rows.isEmpty()) {
					collector.rows.add(new ArrayList<>());
				}
				return collector.rows;
			}
		} catch (OpenXML4JException | ParserConfigurationException | SAXException ex) {
			SheetRowLimitException limit = findRowLimit(ex);
			if (limit != null) {
				throw limit;
			}
			throw new IOException(ex.getMessage(), ex);
		}
	}

	private static List<List<Object>> readFirstSheetWorkbook(byte[] xlsxBytes, int maxDataRows) throws IOException {
		try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(xlsxBytes))) {
			Sheet sheet = wb.getSheetAt(0);
			List<List<Object>> rows = new ArrayList<>();
			int last = sheet.getLastRowNum();
			for (int r = 0; r <= last; r++) {
				if (r > maxDataRows) {
					throw new SheetRowLimitException(maxDataRows);
				}
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

	private static SheetRowLimitException findRowLimit(Throwable ex) {
		Throwable current = ex;
		while (current != null) {
			if (current instanceof SheetRowLimitException limit) {
				return limit;
			}
			current = current.getCause();
		}
		return null;
	}

	/** 数据行超过调用方给出的上限。表头不计入。 */
	public static final class SheetRowLimitException extends IOException {

		public SheetRowLimitException(int maxDataRows) {
			super("数据行超过上限" + maxDataRows);
		}
	}

	private static final class SheetCollector implements XSSFSheetXMLHandler.SheetContentsHandler {

		private final int maxDataRows;
		private final List<List<Object>> rows = new ArrayList<>();
		private List<Object> current;

		private SheetCollector(int maxDataRows) {
			this.maxDataRows = maxDataRows;
		}

		@Override
		public void startRow(int rowNum) {
			if (rowNum > maxDataRows) {
				throw new RowLimitSignal(new SheetRowLimitException(maxDataRows));
			}
			while (rows.size() < rowNum) {
				rows.add(new ArrayList<>());
			}
			current = new ArrayList<>();
		}

		@Override
		public void endRow(int rowNum) {
			if (current != null) {
				rows.add(current);
				current = null;
			}
		}

		@Override
		public void cell(String cellReference, String formattedValue, XSSFComment comment) {
			if (current == null) {
				current = new ArrayList<>();
			}
			int column = columnIndex(cellReference);
			while (current.size() <= column) {
				current.add("");
			}
			current.set(column, formattedValue == null ? "" : formattedValue);
		}
	}

	private static int columnIndex(String cellReference) {
		if (cellReference == null || cellReference.isEmpty()) {
			return 0;
		}
		int column = 0;
		for (int i = 0; i < cellReference.length(); i++) {
			char ch = cellReference.charAt(i);
			if (ch >= 'A' && ch <= 'Z') {
				column = column * 26 + (ch - 'A' + 1);
			} else if (ch >= 'a' && ch <= 'z') {
				column = column * 26 + (ch - 'a' + 1);
			} else {
				break;
			}
		}
		return Math.max(column - 1, 0);
	}

	private static final class RowLimitSignal extends RuntimeException {

		private RowLimitSignal(SheetRowLimitException cause) {
			super(cause);
		}
	}
}
