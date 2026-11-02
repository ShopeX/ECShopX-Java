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

public final class PosStoreAccountHeaderInfo {
	private PosStoreAccountHeaderInfo() {}

	public static Map<String, UploadHeaderColumnInfo> map() {
		Map<String, UploadHeaderColumnInfo> map = new LinkedHashMap<>();
		map.put("姓名", new UploadHeaderColumnInfo(30, "", true));
		map.put("员工号", new UploadHeaderColumnInfo(30, "公司内唯一，不区分大小写", true));
		map.put("邮箱", new UploadHeaderColumnInfo(255, "标准邮箱格式", true));
		map.put("手机号", new UploadHeaderColumnInfo(20, "", true));
		map.put("角色", new UploadHeaderColumnInfo(16, "店长/主管/店员", true));
		map.put("门店", new UploadHeaderColumnInfo(32, "填店铺号 shop_code", true));
		map.put("状态", new UploadHeaderColumnInfo(16, "启用/禁用/离职", true));
		return Collections.unmodifiableMap(map);
	}
}
