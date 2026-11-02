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

package cn.shopex.ecshopx.companys.service.posaccount;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class PosStoreAccountPasswordGenerator {

	private static final char[] LETTERS =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
	private static final char[] DIGITS = "0123456789".toCharArray();
	private static final char[] ALPHANUM =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

	private final SecureRandom random = new SecureRandom();

	public String generate() {
		char[] out = new char[8];
		out[0] = LETTERS[random.nextInt(LETTERS.length)];
		out[1] = DIGITS[random.nextInt(DIGITS.length)];
		for (int i = 2; i < 8; i++) {
			out[i] = ALPHANUM[random.nextInt(ALPHANUM.length)];
		}
		for (int i = out.length - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			char tmp = out[i];
			out[i] = out[j];
			out[j] = tmp;
		}
		return new String(out);
	}
}
