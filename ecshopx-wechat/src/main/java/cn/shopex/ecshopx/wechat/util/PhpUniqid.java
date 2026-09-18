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

package cn.shopex.ecshopx.wechat.util;

import java.time.Instant;

public final class PhpUniqid {

	private PhpUniqid() {}

	/** 13 位小写十六进制，对齐 PHP {@code uniqid()} 无 prefix 形式。 */
	public static String next() {
		Instant now = Instant.now();
		long sec = now.getEpochSecond();
		int usec = now.getNano() / 1000;
		return String.format("%08x%05x", sec, usec);
	}
}
