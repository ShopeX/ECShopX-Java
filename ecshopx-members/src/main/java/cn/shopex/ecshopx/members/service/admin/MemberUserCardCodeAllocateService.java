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

package cn.shopex.ecshopx.members.service.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MemberUserCardCodeAllocateService {

	private static final char[] B36 =
			"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

	private final StringRedisTemplate membersRedis;

	public MemberUserCardCodeAllocateService(
			@Qualifier("membersStringRedisTemplate") StringRedisTemplate membersRedis) {
		this.membersRedis = membersRedis;
	}

	public String allocateCode() {
		String code = null;
		for (int i = 1; i <= 3; i++) {
			code = genCode("", 12);
			Boolean ok = membersRedis.opsForZSet().add("usercardcode", code, 1.0);
			if (Boolean.TRUE.equals(ok)) {
				return code;
			}
		}
		throw new IllegalStateException("会员卡卡号生成失败（Redis）");
	}

	private static String genCode(String prefix, int length) {
		String iNo = dec2b36(999999);
		if (iNo.length() < length) {
			iNo = "0".repeat(length - iNo.length()) + iNo;
		}
		int leftLength = length;
		if (prefix != null && !prefix.isEmpty()) {
			leftLength = length - prefix.length();
		}
		String sha1Hex = sha1HexUtf8(prefix + iNo);
		int start = ThreadLocalRandom.current().nextInt(0, 27);
		int end = Math.min(start + leftLength, sha1Hex.length());
		String slice = sha1Hex.substring(start, end);
		String shuffled = shuffleString(slice);
		return prefix + shuffled.toUpperCase();
	}

	private static String dec2b36(int n) {
		if (n <= 0) {
			return "0";
		}
		StringBuilder ret = new StringBuilder();
		int v = n;
		while (v > 0) {
			ret.insert(0, B36[v % 36]);
			v = v / 36;
		}
		return ret.toString();
	}

	private static String shuffleString(String s) {
		char[] arr = s.toCharArray();
		ThreadLocalRandom r = ThreadLocalRandom.current();
		for (int i = arr.length - 1; i > 0; i--) {
			int j = r.nextInt(i + 1);
			char t = arr[i];
			arr[i] = arr[j];
			arr[j] = t;
		}
		return new String(arr);
	}

	private static String sha1HexUtf8(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
