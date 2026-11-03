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

package cn.shopex.ecshopx.companys.service.posaccount;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PosStoreAccountStatusService {

	private final PosStoreAccountMapper mapper;
	private final PosStoreAccountQueryService queryService;

	public PosStoreAccountStatusService(
			PosStoreAccountMapper mapper, PosStoreAccountQueryService queryService) {
		this.mapper = mapper;
		this.queryService = queryService;
	}

	public Map<String, Object> change(Map<String, Object> jwt, long id, String action) {
		String target = targetStatus(action);
		PosStoreAccount row = queryService.requireReadable(jwt, id);
		apply(row, target, false);
		row.setUpdated((int) (System.currentTimeMillis() / 1000L));
		mapper.updateById(row);
		return PosStoreAccountQueryService.toPublicRow(row, null);
	}

	public Map<String, Object> batch(Map<String, Object> jwt, List<Long> ids, String action) {
		String target = targetStatus(action);
		if (ids == null || ids.isEmpty()) {
			throw new BadRequestException("请选择账号");
		}
		List<Long> success = new ArrayList<>();
		List<Long> skipped = new ArrayList<>();
		List<Map<String, Object>> failed = new ArrayList<>();
		for (Long id : ids) {
			if (id == null || id <= 0) {
				continue;
			}
			try {
				PosStoreAccount row = queryService.requireReadable(jwt, id);
				String result = apply(row, target, true);
				if ("skip".equals(result)) {
					skipped.add(id);
					continue;
				}
				row.setUpdated((int) (System.currentTimeMillis() / 1000L));
				mapper.updateById(row);
				success.add(id);
			} catch (ForbiddenException e) {
				failed.add(fail(id, e.getMessage()));
			} catch (BadRequestException e) {
				if (e.getMessage() != null && e.getMessage().contains("离职")) {
					skipped.add(id);
				} else {
					failed.add(fail(id, e.getMessage()));
				}
			}
		}
		if (success.isEmpty() && skipped.isEmpty() && failed.isEmpty()) {
			throw new BadRequestException("请选择账号");
		}
		if (success.isEmpty() && failed.isEmpty()) {
			throw new BadRequestException("无可操作账号");
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("success", success);
		out.put("skipped", skipped);
		out.put("failed", failed);
		return out;
	}

	private static String apply(PosStoreAccount row, String target, boolean batch) {
		if (target.equals(row.getStatus())) {
			if (batch) {
				return "skip";
			}
			return "ok";
		}
		if ("resigned".equals(row.getStatus()) && !"resigned".equals(target)) {
			throw new BadRequestException("离职账号不可恢复");
		}
		row.setStatus(target);
		return "ok";
	}

	private static String targetStatus(String action) {
		String a = action == null ? "" : action.trim();
		return switch (a) {
			case "enable" -> "enabled";
			case "disable" -> "disabled";
			case "resign" -> "resigned";
			default -> throw new BadRequestException("状态无效");
		};
	}

	private static Map<String, Object> fail(long id, String reason) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", id);
		m.put("reason", reason == null ? "失败" : reason);
		return m;
	}
}
