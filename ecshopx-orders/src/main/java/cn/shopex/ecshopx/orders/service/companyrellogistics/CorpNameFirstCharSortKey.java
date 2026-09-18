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

package cn.shopex.ecshopx.orders.service.companyrellogistics;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

/**
 * GB2312 双字节区段到 A–Z 的排序键，用于公司名称的多列排序键（字符串词典序）。
 */
public final class CorpNameFirstCharSortKey {

	private static final Charset GB2312 = Charset.forName("GB2312");

	private CorpNameFirstCharSortKey() {}

	/**
	 * 供列表排序：无法归类时返回空串，使未识别字符排在最前（与空排序键行为一致）。
	 */
	public static String sortKeyForMultisort(String str) {
		String key = charterKeyOrNull(str);
		return key == null ? "" : key;
	}

	private static String charterKeyOrNull(String str) {
		if (str == null || str.isEmpty()) {
			return "";
		}
		int cp = str.codePointAt(0);
		if (Character.isDigit(cp)) {
			return new String(Character.toChars(cp));
		}
		if (cp >= 'A' && cp <= 'Z') {
			return String.valueOf((char) cp);
		}
		if (cp >= 'a' && cp <= 'z') {
			return String.valueOf((char) (cp - 'a' + 'A'));
		}
		String first = new String(Character.toChars(cp));
		byte[] gb;
		try {
			gb = encodeStrictGb2312(first);
		} catch (CharacterCodingException e) {
			return null;
		}
		if (gb.length < 2) {
			return null;
		}
		int b0 = gb[0] & 0xFF;
		int b1 = gb[1] & 0xFF;
		int asc = b0 * 256 + b1 - 65536;
		return mapAscToLetterOrNull(asc);
	}

	private static byte[] encodeStrictGb2312(String s) throws CharacterCodingException {
		CharsetEncoder enc =
				GB2312.newEncoder()
						.onMalformedInput(CodingErrorAction.REPORT)
						.onUnmappableCharacter(CodingErrorAction.REPORT);
		ByteBuffer bb = enc.encode(CharBuffer.wrap(s));
		byte[] out = new byte[bb.remaining()];
		bb.get(out);
		return out;
	}

	private static String mapAscToLetterOrNull(int asc) {
		if (asc >= -20319 && asc <= -20284) {
			return "A";
		}
		if (asc >= -20283 && asc <= -19776) {
			return "B";
		}
		if (asc >= -19775 && asc <= -19219) {
			return "C";
		}
		if (asc >= -19218 && asc <= -18711) {
			return "D";
		}
		if (asc >= -18710 && asc <= -18527) {
			return "E";
		}
		if (asc >= -18526 && asc <= -18240) {
			return "F";
		}
		if (asc >= -18239 && asc <= -17923) {
			return "G";
		}
		if (asc >= -17922 && asc <= -17418) {
			return "H";
		}
		if (asc >= -17417 && asc <= -16475) {
			return "J";
		}
		if (asc >= -16474 && asc <= -16213) {
			return "K";
		}
		if (asc >= -16212 && asc <= -15641) {
			return "L";
		}
		if (asc >= -15640 && asc <= -15166) {
			return "M";
		}
		if (asc >= -15165 && asc <= -14923) {
			return "N";
		}
		if (asc >= -14922 && asc <= -14915) {
			return "O";
		}
		if (asc >= -14914 && asc <= -14631) {
			return "P";
		}
		if (asc >= -14630 && asc <= -14150) {
			return "Q";
		}
		if (asc >= -14149 && asc <= -14091) {
			return "R";
		}
		if (asc >= -14090 && asc <= -13319) {
			return "S";
		}
		if (asc >= -13318 && asc <= -12839) {
			return "T";
		}
		if (asc >= -12838 && asc <= -12557) {
			return "W";
		}
		if (asc >= -12556 && asc <= -11848) {
			return "X";
		}
		if (asc >= -11847 && asc <= -11056) {
			return "Y";
		}
		if (asc >= -11055 && asc <= -10247) {
			return "Z";
		}
		return null;
	}
}
