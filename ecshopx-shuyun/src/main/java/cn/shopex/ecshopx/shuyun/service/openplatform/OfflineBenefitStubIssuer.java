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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendBatch;
import cn.shopex.ecshopx.shuyun.domain.ShuyunOfflineBenefitSendItem;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

/**
 * 联调占位：确定性伪券码。对齐 PHP {@code ShuyunOfflineBenefitStubIssuer}。
 */
@Component
public class OfflineBenefitStubIssuer implements OfflineBenefitItemIssuer {

	@Override
	public OfflineBenefitIssueResult issue(
			ShuyunOfflineBenefitSendBatch batch, ShuyunOfflineBenefitSendItem item) {
		String raw = item.getCustomerId() + ":" + batch.getBenefitId();
		String suffix = sha256Hex(raw).substring(0, 12);
		return OfflineBenefitIssueResult.ok("STUB-" + suffix, null);
	}

	private static String sha256Hex(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
