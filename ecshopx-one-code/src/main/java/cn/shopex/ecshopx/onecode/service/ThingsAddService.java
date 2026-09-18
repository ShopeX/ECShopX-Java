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

package cn.shopex.ecshopx.onecode.service;

import cn.shopex.ecshopx.onecode.domain.Things;
import cn.shopex.ecshopx.onecode.mapper.ThingsMapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThingsAddService {

	private final ThingsMapper thingsMapper;

	public ThingsAddService(ThingsMapper thingsMapper) {
		this.thingsMapper = thingsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> addThings(long companyId, String thingName, String pic, int priceInCents, String intro) {
		int now = (int) (System.currentTimeMillis() / 1000);

		Things entity = new Things();
		entity.setCompanyId(companyId);
		entity.setThingName(thingName);
		entity.setPrice(priceInCents);
		entity.setPic(pic);
		entity.setIntro(intro);
		entity.setCreated(now);
		entity.setUpdated(now);

		thingsMapper.insert(entity);

		return ThingsRowMapSupport.toThingRowMap(entity);
	}
}
