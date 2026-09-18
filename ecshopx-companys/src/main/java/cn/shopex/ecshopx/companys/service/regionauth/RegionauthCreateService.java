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

package cn.shopex.ecshopx.companys.service.regionauth;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Regionauth;
import cn.shopex.ecshopx.companys.mapper.RegionauthMapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class RegionauthCreateService {

	private final RegionauthMapper regionauthMapper;

	public RegionauthCreateService(RegionauthMapper regionauthMapper) {
		this.regionauthMapper = regionauthMapper;
	}

	public void create(Map<String, Object> body, long companyId) {
		Object nameRaw = body.get("regionauth_name");
		String name = stringValue(nameRaw).trim();
		if (name.isEmpty()) {
			throw new BadRequestException("地区名称不能为空");
		}
		if (name.length() > 50) {
			throw new BadRequestException("地区名称长度不能超过50");
		}

		int now = (int) Instant.now().getEpochSecond();
		Regionauth entity = new Regionauth();
		entity.setRegionauthName(name);
		entity.setCompanyId(companyId);
		entity.setState(Integer.valueOf(1));
		entity.setCreated(now);
		entity.setUpdated(now);

		try {
			regionauthMapper.insert(entity);
		} catch (DataIntegrityViolationException e) {
			throw new ResourceException("操作失败");
		}

		Long id = entity.getRegionauthId();
		if (id == null || id <= 0L) {
			throw new ResourceException("操作失败");
		}
	}

	private static String stringValue(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof String s) {
			return s;
		}
		return String.valueOf(o);
	}
}
