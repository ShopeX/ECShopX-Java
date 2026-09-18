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

import cn.shopex.ecshopx.companys.domain.Regionauth;
import cn.shopex.ecshopx.companys.mapper.RegionauthMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RegionauthDetailService {

	private final RegionauthMapper regionauthMapper;

	public RegionauthDetailService(RegionauthMapper regionauthMapper) {
		this.regionauthMapper = regionauthMapper;
	}

	public Map<String, Object> getinfo(long companyId, long regionauthId) {
		LambdaQueryWrapper<Regionauth> w = new LambdaQueryWrapper<>();
		w.eq(Regionauth::getCompanyId, companyId);
		w.eq(Regionauth::getRegionauthId, regionauthId);
		w.in(Regionauth::getState, List.of(0, 1));
		Regionauth row = regionauthMapper.selectOne(w);
		if (row == null) {
			throw new ResourceException("数据不存在");
		}
		LinkedHashMap<String, Object> line = new LinkedHashMap<>();
		line.put("regionauth_id", row.getRegionauthId());
		line.put("regionauth_name", row.getRegionauthName());
		line.put("state", row.getState());
		line.put("company_id", row.getCompanyId());
		line.put("created", row.getCreated());
		line.put("updated", row.getUpdated());
		return line;
	}
}
