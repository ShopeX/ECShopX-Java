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

public final class AdapayTradedataHeaderInfo {
	private AdapayTradedataHeaderInfo() {}

	public static Map<String, UploadHeaderColumnInfo> map() {
		Map<String, UploadHeaderColumnInfo> map = new LinkedHashMap<>();
		map.put("订单号", new UploadHeaderColumnInfo(32, "不得重复，订单号如果大于11位时，请关闭excel单元格的科学记数法，常用禁用方法：“单元格格式”-“自定义”-“类型”改为“0”", true));
		map.put("交易单号", new UploadHeaderColumnInfo(32, "不得重复，交易单号如果大于11位时，请关闭excel单元格的科学记数法，常用禁用方法：“单元格格式”-“自定义”-“类型”改为“0”", true));
		map.put("是否分账", new UploadHeaderColumnInfo(30, "已分账/未分账", true));
		return Collections.unmodifiableMap(map);
	}
}
