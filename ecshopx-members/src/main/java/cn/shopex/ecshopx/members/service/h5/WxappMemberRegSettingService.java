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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.members.integration.h5.H5WxappMemberRegSettingPort;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WxappMemberRegSettingService {

	private final H5WxappMemberRegSettingPort h5WxappMemberRegSettingPort;

	public Object getRegSetting(long companyId, boolean editedPage, String acceptLanguageHeader) {
		if (!editedPage) {
			return Collections.emptyList();
		}
		return h5WxappMemberRegSettingPort.loadWxappMemberRegSettingByKeyName(
				companyId, acceptLanguageHeader);
	}
}
