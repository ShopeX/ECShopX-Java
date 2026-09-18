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

package cn.shopex.ecshopx.chinaumspay.service.transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 回盘 .ret 文本与指令 ID 解析；供 {@link ChinaumsPayDivisionDoTransferDownloadSftpService} 使用。
 */
public final class ChinaumsPayDivisionRetResponseCodec {

	public static final String STATUS_FILE_ERROR = "file_error";
	public static final String STATUS_SUCC = "succ";

	private static final int FIRST_LINE_COLUMN_COUNT = 12;
	/** 明细列（去掉 10、11 两列后）与 dataCol 标题一一对应。 */
	private static final int DATA_FIELD_COUNT = 10;
	private static final int DATA_COL_UNSET_A = 10;
	private static final int DATA_COL_UNSET_B = 11;

	/** 汇总行与 {@code dataCol} 的键名顺序（与下标无关，仅作调试可读）。 */
	private static final String[] DATA_COL_KEYS = {
		"id",
		"enterpriseid",
		"type",
		"fee",
		"succ_fee",
		"status",
		"status_msg",
		"chinaumspay_id",
		"final_fee",
		"rate_fee"
	};

	private static final String[] FILE_ERROR_CODES = {"VERIFY_FAILED", "GROUPNO_ERROR", "TOTAL_INSTRUCTION_NOT_MATCH"};

	private static final String[] FILE_ERROR_MSG = {"验签失败", "集团号不一致", "汇总数与明细条数不一致"};

	private ChinaumsPayDivisionRetResponseCodec() {}

	/**
	 * 将银联行内状态位映射为本地表 {@code back_status} 字面值（0/1/2/3/4）；未识别时与 PHP 的 {@code ?? 0}
	 * 一致为未处理字面值 {@code "0"}（非失败码）。
	 */
	public static String mapUmsStatusCharToLocalBackStatus(String ums) {
		if (ums == null || ums.isEmpty()) {
			return "0";
		}
		return switch (ums) {
			case "0" -> "4";
			case "1" -> "2";
			case "2" -> "3";
			case "3" -> "1";
			default -> "0";
		};
	}

	/**
	 * 将合并指令 id 反解为 (division, uploadDetail, times)，与上送时 {@code div + '0' + detail + '0' + times} 为逆。
	 * 在有多解时，优先更长的首段以消除歧义（与 PHP 行为一致、且与上送 toString 口径对齐）。
	 */
	public static long[] parseRetIdTriple(String composite) {
		if (!StringUtils.hasText(composite)) {
			return new long[] {0L, 0L, 0L};
		}
		int n = composite.length();
		// 自左而右找**第一个**可三分且 round-trip 与 composite 等长的分隔，避免「更长首段 + 后段 5+6」的歧义
		//（与上送 a+0+b+0+c 且先拆左边界一致）。
		for (int i = 0; i < n; i++) {
			if (composite.charAt(i) != '0') {
				continue;
			}
			String a = composite.substring(0, i);
			if (a.isEmpty() || !allDigit(a)) {
				continue;
			}
			String tail = composite.substring(i + 1);
			if (tail.isEmpty()) {
				continue;
			}
			// 自右而左，避免 4050|6 被拆成 4|506 的歧义
			for (int j = tail.length() - 1; j > 0; j--) {
				if (tail.charAt(j) != '0') {
					continue;
				}
				String b = tail.substring(0, j);
				String c = tail.substring(j + 1);
				if (b.isEmpty() || c.isEmpty()) {
					continue;
				}
				if (!allDigit(b) || !allDigit(c)) {
					continue;
				}
				if (hasRedundantLeadingZero(b) || hasRedundantLeadingZero(c)) {
					continue;
				}
				if ((a + "0" + b + "0" + c).equals(composite)) {
					return new long[] {Long.parseLong(a), Long.parseLong(b), Long.parseLong(c)};
				}
			}
		}
		return new long[] {0L, 0L, 0L};
	}

	private static boolean allDigit(String s) {
		for (int k = 0; k < s.length(); k++) {
			if (!Character.isDigit(s.charAt(k))) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasRedundantLeadingZero(String s) {
		return s.length() > 1 && s.charAt(0) == '0';
	}

	/**
	 * 解析日终 .ret 全文。{@code fileError} 为 true 时仅 {@code fileErrorMessage} 有效；无明细时 {@code
	 * dataRows} 可能为空表。
	 */
	public static FormattedRet parseFileContent(String content) {
		if (content == null) {
			FormattedRet r = new FormattedRet();
			r.status = STATUS_SUCC;
			r.dataRows = Collections.emptyList();
			return r;
		}
		String[] lines = content.split("\n", -1);
		if (lines.length == 0) {
			FormattedRet r = new FormattedRet();
			r.status = STATUS_SUCC;
			r.dataRows = Collections.emptyList();
			return r;
		}
		String firstCol = lines[0].trim();
		if (firstCol.isEmpty()) {
			FormattedRet r = new FormattedRet();
			r.status = STATUS_SUCC;
			r.dataRows = Collections.emptyList();
			return r;
		}
		String fe = fileErrorMessage(firstCol);
		if (fe != null) {
			FormattedRet r = new FormattedRet();
			r.status = STATUS_FILE_ERROR;
			r.fileErrorMessage = fe;
			r.dataRows = Collections.emptyList();
			return r;
		}
		String[] totalParts = firstCol.split("\\|", -1);
		if (totalParts.length != FIRST_LINE_COLUMN_COUNT) {
			throw new IllegalStateException("回盘首行列数与约定不符: " + totalParts.length);
		}
		FormattedRet out = new FormattedRet();
		out.status = STATUS_SUCC;
		out.dataRows = new ArrayList<>();
		for (int li = 1; li < lines.length; li++) {
			if (!StringUtils.hasText(lines[li]) || lines[li].isBlank()) {
				continue;
			}
			String[] p = lines[li].split("\\|", -1);
			if (p.length < 12) {
				continue;
			}
			List<String> work = new ArrayList<>();
			Collections.addAll(work, p);
			if (work.size() > DATA_COL_UNSET_B) {
				work.remove(DATA_COL_UNSET_B);
			}
			if (work.size() > DATA_COL_UNSET_A) {
				work.remove(DATA_COL_UNSET_A);
			}
			if (work.size() != DATA_FIELD_COUNT) {
				throw new IllegalStateException("回盘明细行列数与表头结合后条数不一致: " + work.size());
			}
			LinkedHashMap<String, String> row = new LinkedHashMap<>();
			for (int j = 0; j < DATA_FIELD_COUNT; j++) {
				row.put(DATA_COL_KEYS[j], nvl(work.get(j)));
			}
			out.dataRows.add(row);
		}
		return out;
	}

	private static String nvl(String s) {
		return s == null ? "" : s;
	}

	/** 首列错误码，命中则整文件为 file_error 语义；否则返回 null。 */
	private static String fileErrorMessage(String firstLineFirstCol) {
		String t = firstLineFirstCol == null ? "" : firstLineFirstCol.trim();
		for (int k = 0; k < FILE_ERROR_CODES.length; k++) {
			if (FILE_ERROR_CODES[k].equals(t)) {
				return FILE_ERROR_MSG[k];
			}
		}
		return null;
	}

	public static final class FormattedRet {
		public String status = STATUS_SUCC;
		/** 当 {@link #status} 为 {@value STATUS_FILE_ERROR} 时。 */
		public String fileErrorMessage;
		/** 明细行，键为 {@link #DATA_COL_KEYS}。 */
		public List<Map<String, String>> dataRows = Collections.emptyList();
	}
}
