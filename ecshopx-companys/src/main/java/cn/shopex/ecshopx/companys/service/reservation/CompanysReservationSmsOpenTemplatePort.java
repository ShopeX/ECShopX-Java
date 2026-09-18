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

package cn.shopex.ecshopx.companys.service.reservation;

import cn.shopex.ecshopx.reservation.port.ReservationSmsOpenTemplatePort;
import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 预约短信模版开关查询；未开启或暂无实现时返回空 Map，调用方按空集合视为未配置模版。
 */
@Service
public class CompanysReservationSmsOpenTemplatePort implements ReservationSmsOpenTemplatePort {

	@Override
	public Map<String, Object> getOpenTemplateInfo(long companyId, String templateKey) {
		return Collections.emptyMap();
	}
}
