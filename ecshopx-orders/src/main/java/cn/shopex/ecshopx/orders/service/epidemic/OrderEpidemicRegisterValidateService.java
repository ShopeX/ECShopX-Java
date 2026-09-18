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

package cn.shopex.ecshopx.orders.service.epidemic;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderEpidemicRegisterValidateService {

	private static final Pattern CN_MOBILE = Pattern.compile("^1\\d{10}$");
	private static final Pattern CN_ID_CARD =
			Pattern.compile("^[1-9]\\d{5}(19|20)\\d{2}[01]\\d[0123]\\d\\d{3}[xX\\d]$");

	public void validate(Map<String, Object> epidemicRegisterInfo) {
		if (epidemicRegisterInfo == null || epidemicRegisterInfo.isEmpty()) {
			throw new BadRequestException("疫情登记信息无效");
		}
		String name = stringVal(epidemicRegisterInfo.get("name"));
		if (!StringUtils.hasText(name)) {
			throw new BadRequestException("登记姓名必填");
		}
		String mobile = stringVal(epidemicRegisterInfo.get("mobile"));
		if (!CN_MOBILE.matcher(mobile).matches()) {
			throw new BadRequestException("登记手机号格式错误");
		}
		String certId = stringVal(epidemicRegisterInfo.get("cert_id"));
		if (!CN_ID_CARD.matcher(certId).matches()) {
			throw new BadRequestException("登记身份证号格式错误");
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
