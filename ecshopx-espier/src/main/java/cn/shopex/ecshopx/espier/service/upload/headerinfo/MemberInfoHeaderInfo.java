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

public final class MemberInfoHeaderInfo {
	private MemberInfoHeaderInfo() {}

	public static Map<String, UploadHeaderColumnInfo> map() {
		Map<String, UploadHeaderColumnInfo> map = new LinkedHashMap<>();
		map.put("会员手机号", new UploadHeaderColumnInfo(32, "不得重复，手机号如果大于11位时，请关闭excel单元格的科学记数法，常用禁用方法：“单元格格式”-“自定义”-“类型”改为“0”", true));
		map.put("原实体卡号", new UploadHeaderColumnInfo(20, "不得重复", false));
		map.put("姓名", new UploadHeaderColumnInfo(20, "", true));
		map.put("性别", new UploadHeaderColumnInfo(2, "性别只能为男,女，未知", true));
		map.put("会员等级", new UploadHeaderColumnInfo(8, "会员等级需和在会员卡中配置的会员等级一致", true));
		map.put("生日", new UploadHeaderColumnInfo(10, "生日时间不得大于今日，格式为mm/dd/yyyy, 如:1/12/2019", false));
		map.put("入会日期", new UploadHeaderColumnInfo(10, "入会时间不得大于今日，格式为mm/dd/yyyy, 如:12/1/2019", true));
		map.put("邮箱", new UploadHeaderColumnInfo(32, "", false));
		map.put("地址", new UploadHeaderColumnInfo(128, "", false));
		map.put("标签", new UploadHeaderColumnInfo(128, "标签名称多个用逗号“,”隔开(注：逗号为半角逗号),并且标签必须已存在系统中,例子：时尚,超级会员", false));
		map.put("积分", new UploadHeaderColumnInfo(32, "会员初始积分", false));
		return Collections.unmodifiableMap(map);
	}
}
