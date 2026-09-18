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

package cn.shopex.ecshopx.espier.service.upload.headerinfo;

import cn.shopex.ecshopx.espier.service.upload.UploadHeaderColumnInfo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EmployeePurchaseEmployeesHeaderInfo {
	private EmployeePurchaseEmployeesHeaderInfo() {}

	public static Map<String, UploadHeaderColumnInfo> map() {
		Map<String, UploadHeaderColumnInfo> map = new LinkedHashMap<>();
		map.put("企业编码", new UploadHeaderColumnInfo(20, "仅支持英文大写字母和数字，可以内购企业列表查询", false));
		map.put("姓名", new UploadHeaderColumnInfo(20, "", true));
		map.put("手机号码", new UploadHeaderColumnInfo(32, "企业登录方式为手机号登录时必填", false));
		map.put("账号", new UploadHeaderColumnInfo(20, "企业登录方式为账号登录时必填", false));
		map.put("校验密码", new UploadHeaderColumnInfo(20, "企业登录方式为账号登录时必填", false));
		map.put("邮箱", new UploadHeaderColumnInfo(20, "企业登录方式为邮箱登录时必填", false));
		return Collections.unmodifiableMap(map);
	}
}
