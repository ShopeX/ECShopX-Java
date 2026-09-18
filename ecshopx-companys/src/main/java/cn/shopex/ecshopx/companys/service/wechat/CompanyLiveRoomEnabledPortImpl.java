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

package cn.shopex.ecshopx.companys.service.wechat;

import cn.shopex.ecshopx.common.wechat.port.CompanyLiveRoomEnabledPort;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanyLiveRoomEnabledPortImpl implements CompanyLiveRoomEnabledPort {

	private static final Logger log = LoggerFactory.getLogger(CompanyLiveRoomEnabledPortImpl.class);

	private final CompanysMapper companysMapper;
	private final ObjectMapper objectMapper;

	public CompanyLiveRoomEnabledPortImpl(CompanysMapper companysMapper, ObjectMapper objectMapper) {
		this.companysMapper = companysMapper;
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean isLiveRoomEnabled(long companyId) {
		Companys row = companysMapper.selectById(companyId);
		if (row == null) {
			return false;
		}
		String tp = row.getThirdParams();
		if (!StringUtils.hasText(tp)) {
			return false;
		}
		try {
			JsonNode root = objectMapper.readTree(tp);
			if (!root.isObject()) {
				return false;
			}
			JsonNode n = root.path("is_liveroom");
			if (n.isMissingNode() || n.isNull()) {
				return false;
			}
			if (n.isBoolean()) {
				return n.booleanValue();
			}
			if (n.isNumber()) {
				return n.asInt() != 0;
			}
			if (n.isTextual()) {
				String s = n.asText("").trim();
				if (s.isEmpty()) {
					return false;
				}
				String lower = s.toLowerCase();
				return "1".equals(lower) || "true".equals(lower);
			}
			return false;
		} catch (Exception e) {
			log.warn("third_params JSON 解析失败，按未开启直播处理: companyId={}", companyId, e);
			return false;
		}
	}
}
