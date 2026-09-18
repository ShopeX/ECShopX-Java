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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.members.admin.AdminMemberSetMemberSalesmanPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberSetMemberSalesmanService {

	private final ObjectMapper objectMapper;
	private final AdminMemberSetMemberSalesmanPort adminMemberSetMemberSalesmanPort;

	public AdminMemberSetMemberSalesmanService(
			ObjectMapper objectMapper, AdminMemberSetMemberSalesmanPort adminMemberSetMemberSalesmanPort) {
		this.objectMapper = objectMapper;
		this.adminMemberSetMemberSalesmanPort = adminMemberSetMemberSalesmanPort;
	}

	public void setMemberSalesman(long companyId, Long distributorId, Map<String, Object> merged) {
		List<Long> userIds = new ArrayList<>();
		Object raw = merged.get("user_ids");
		if (raw == null) {
			throw new BadRequestException("会员id必填");
		}
		if (raw instanceof String str) {
			if (!StringUtils.hasText(str.trim())) {
				throw new BadRequestException("会员id必填");
			}
			try {
				JsonNode root = objectMapper.readTree(str.trim());
				if (!root.isArray()) {
					throw new BadRequestException("会员id必填");
				}
				for (JsonNode el : root) {
					collectUserId(el, userIds);
				}
			} catch (JsonProcessingException e) {
				throw new BadRequestException("会员id必填");
			}
		} else if (raw instanceof Collection<?> col) {
			for (Object el : col) {
				collectUserId(el, userIds);
			}
		} else if (raw instanceof Object[] arr) {
			for (Object el : arr) {
				collectUserId(el, userIds);
			}
		} else {
			throw new BadRequestException("会员id必填");
		}

		Object sRaw = merged.get("salesman_id");
		if (sRaw == null || !StringUtils.hasText(String.valueOf(sRaw).trim())) {
			throw new BadRequestException("导购员必填");
		}
		long salesmanId;
		try {
			salesmanId = Long.parseLong(String.valueOf(sRaw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("导购员必填");
		}

		adminMemberSetMemberSalesmanPort.setMemberSalesman(companyId, distributorId, userIds, salesmanId);
	}

	private void collectUserId(Object element, List<Long> userIds) {
		if (element instanceof JsonNode jn) {
			if (!jn.isObject()) {
				throw new BadRequestException("会员id必填");
			}
			if (!jn.has("user_id") || jn.get("user_id").isNull()) {
				throw new BadRequestException("会员id必填");
			}
			JsonNode uidNode = jn.get("user_id");
			if (uidNode.isObject() || uidNode.isArray()) {
				throw new BadRequestException("会员id必填");
			}
			String uidStr = uidNode.asText();
			if (!StringUtils.hasText(uidStr != null ? uidStr.trim() : "")) {
				throw new BadRequestException("会员id必填");
			}
			long userId;
			try {
				userId = Long.parseLong(uidStr.trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("会员id必填");
			}
			if (userId <= 0L) {
				throw new BadRequestException("会员id必填");
			}
			userIds.add(userId);
			return;
		}
		if (element instanceof Map<?, ?> m) {
			if (!m.containsKey("user_id")) {
				throw new BadRequestException("会员id必填");
			}
			Object v = m.get("user_id");
			if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
				throw new BadRequestException("会员id必填");
			}
			long userId;
			try {
				userId = Long.parseLong(String.valueOf(v).trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("会员id必填");
			}
			if (userId <= 0L) {
				throw new BadRequestException("会员id必填");
			}
			userIds.add(userId);
			return;
		}
		if (element instanceof Number) {
			throw new BadRequestException("会员id必填");
		}
		throw new BadRequestException("会员id必填");
	}
}
