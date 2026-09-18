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

package cn.shopex.ecshopx.espier.integration.members;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.espier.service.config.ConfigRequestFieldsApplicationService;
import cn.shopex.ecshopx.members.integration.h5.H5WxappMemberRegSettingPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service("h5WxappMemberRegSettingPortImpl")
public class H5WxappMemberRegSettingPortImpl implements H5WxappMemberRegSettingPort {

	private final ConfigRequestFieldsApplicationService configRequestFieldsApplicationService;

	public H5WxappMemberRegSettingPortImpl(
			ConfigRequestFieldsApplicationService configRequestFieldsApplicationService) {
		this.configRequestFieldsApplicationService = configRequestFieldsApplicationService;
	}

	@Override
	public LinkedHashMap<String, Map<String, Object>> loadWxappMemberRegSettingByKeyName(
			long companyId, String acceptLanguageHeader) {
		int cid;
		try {
			cid = Math.toIntExact(companyId);
		} catch (ArithmeticException e) {
			throw new BadRequestException("参数 company_id 错误");
		}
		return configRequestFieldsApplicationService.loadWxappMemberRegSettingByKeyName(
				cid, acceptLanguageHeader);
	}
}
