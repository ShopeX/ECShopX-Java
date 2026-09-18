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

package cn.shopex.ecshopx.adapay.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdapayOpenAccountOtherCatService {

	private static final LinkedHashMap<String, String> FEE_ENTRY_01;
	private static final LinkedHashMap<String, String> FEE_ENTRY_02;
	private static final LinkedHashMap<String, String> MODEL_ENTRY_1;
	private static final List<LinkedHashMap<String, String>> MER_TYPE_ALL;

	static {
		FEE_ENTRY_01 = new LinkedHashMap<>();
		FEE_ENTRY_01.put("code", "01");
		FEE_ENTRY_01.put("name", "标准费率线上");

		FEE_ENTRY_02 = new LinkedHashMap<>();
		FEE_ENTRY_02.put("code", "02");
		FEE_ENTRY_02.put("name", "标准费率线下");

		MODEL_ENTRY_1 = new LinkedHashMap<>();
		MODEL_ENTRY_1.put("code", "1");
		MODEL_ENTRY_1.put("name", "服务商模式");

		MER_TYPE_ALL = new ArrayList<>();

		LinkedHashMap<String, String> mer1 = new LinkedHashMap<>();
		mer1.put("code", "1");
		mer1.put("name", "政府机构");
		MER_TYPE_ALL.add(mer1);

		LinkedHashMap<String, String> mer2 = new LinkedHashMap<>();
		mer2.put("code", "2");
		mer2.put("name", "国营企业");
		MER_TYPE_ALL.add(mer2);

		LinkedHashMap<String, String> mer3 = new LinkedHashMap<>();
		mer3.put("code", "3");
		mer3.put("name", "私营企业");
		MER_TYPE_ALL.add(mer3);

		LinkedHashMap<String, String> mer4 = new LinkedHashMap<>();
		mer4.put("code", "4");
		mer4.put("name", "外资企业");
		MER_TYPE_ALL.add(mer4);

		LinkedHashMap<String, String> mer5 = new LinkedHashMap<>();
		mer5.put("code", "5");
		mer5.put("name", "个体工商户");
		MER_TYPE_ALL.add(mer5);

		LinkedHashMap<String, String> mer7 = new LinkedHashMap<>();
		mer7.put("code", "7");
		mer7.put("name", "事业单位");
		MER_TYPE_ALL.add(mer7);

		LinkedHashMap<String, String> mer8 = new LinkedHashMap<>();
		mer8.put("code", "8");
		mer8.put("name", "小微");
		MER_TYPE_ALL.add(mer8);
	}

	public Map<String, Object> otherCat(String merchantTypeName) {
		List<Map<String, String>> feeType =
				new ArrayList<>(Arrays.asList(FEE_ENTRY_01, FEE_ENTRY_02));
		List<Map<String, String>> modelType = new ArrayList<>(Arrays.asList(MODEL_ENTRY_1));
		List<Map<String, String>> merType = new ArrayList<>();

		for (LinkedHashMap<String, String> value : MER_TYPE_ALL) {
			if ("企业".equals(merchantTypeName)
					&& ("2".equals(value.get("code")) || "3".equals(value.get("code")))) {
				merType.add(new LinkedHashMap<>(value));
			}
			if ("个体户".equals(merchantTypeName) && "5".equals(value.get("code"))) {
				merType.add(new LinkedHashMap<>(value));
			}
			String code = value.get("code");
			if ("政府事业单位".equals(merchantTypeName) && ("1".equals(code) || "7".equals(code))) {
				merType.add(new LinkedHashMap<>(value));
			}
			if ("小微商户".equals(merchantTypeName) && "8".equals(code)) {
				merType.add(new LinkedHashMap<>(value));
			}
			if ("其他组织".equals(merchantTypeName) && "4".equals(code)) {
				merType.add(new LinkedHashMap<>(value));
			}
		}

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("fee_type", feeType);
		res.put("model_type", modelType);
		res.put("mer_type", merType);
		return res;
	}
}
