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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.distribution.CompanyMapGeocodePort;
import java.util.Collections;
import org.springframework.stereotype.Service;

@Service
public class DistributorH5GetAreaByJwdService {

	private final CompanyMapGeocodePort companyMapGeocodePort;

	public DistributorH5GetAreaByJwdService(CompanyMapGeocodePort companyMapGeocodePort) {
		this.companyMapGeocodePort = companyMapGeocodePort;
	}

	public Object getAreaByJwd(long companyId, String lat, String lng) {
		try {
			return companyMapGeocodePort.getPositionByLatAndLngRaw(companyId, lat, lng);
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}
}
