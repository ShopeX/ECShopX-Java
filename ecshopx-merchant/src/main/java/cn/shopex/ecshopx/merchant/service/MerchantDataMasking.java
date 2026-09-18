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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.util.DataMasking;
import java.util.List;
import java.util.Map;

public final class MerchantDataMasking {

	private MerchantDataMasking() {
	}

	public static void applyOperatorMobileMaskIfNeeded(List<Map<String, Object>> rows, boolean shouldMask) {
		if (!shouldMask || rows == null) {
			return;
		}
		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			Object mobile = row.get("mobile");
			if (mobile != null) {
				row.put("mobile", DataMasking.maskMobile(mobile.toString()));
			}
		}
	}

	public static void applyListIfNeeded(List<Map<String, Object>> rows, boolean shouldMask) {
		if (!shouldMask || rows == null) {
			return;
		}
		for (Map<String, Object> row : rows) {
			if (row == null) {
				continue;
			}
			Object ln = row.get("legal_name");
			if (ln != null) {
				row.put("legal_name", DataMasking.maskTruename(ln.toString()));
			}
			Object lm = row.get("legal_mobile");
			if (lm != null) {
				row.put("legal_mobile", DataMasking.maskMobile(lm.toString()));
			}
		}
	}

	public static void applyDetail(Map<String, Object> row) {
		Object cert = row.get("legal_cert_id");
		if (cert != null) {
			String masked = DataMasking.maskIdcard(cert.toString());
			if (masked != null) {
				row.put("legal_cert_id", masked);
			}
		}
		Object lm = row.get("legal_mobile");
		if (lm != null) {
			String masked = DataMasking.maskMobile(lm.toString());
			if (masked != null) {
				row.put("legal_mobile", masked);
			}
		}
		Object bm = row.get("bank_mobile");
		if (bm != null) {
			String masked = DataMasking.maskMobile(bm.toString());
			if (masked != null) {
				row.put("bank_mobile", masked);
			}
		}
		Object card = row.get("card_id_mask");
		if (card != null) {
			String masked = DataMasking.maskBankcard(card.toString());
			if (masked != null) {
				row.put("card_id_mask", masked);
			}
		}
		putImageIfPresent(row, "legal_certid_front_url");
		putImageIfPresent(row, "legal_cert_id_back_url");
		putImageIfPresent(row, "bank_card_front_url");
	}

	private static void putImageIfPresent(Map<String, Object> row, String key) {
		Object v = row.get(key);
		if (v != null) {
			row.put(key, DataMasking.maskImage(String.valueOf(v)));
		}
	}
}
