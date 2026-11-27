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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorBatchStatusService {

	public static final int MAX_BATCH_SIZE = 100;

	private static final Set<String> ALLOWED_IS_VALID =
			Set.of("true", "false", "closed", "delete");

	private final DistributorWriteRepository distributorWriteRepository;
	private final DistributorUpdateOrchestrator distributorUpdateOrchestrator;

	public DistributorBatchStatusService(
			DistributorWriteRepository distributorWriteRepository,
			DistributorUpdateOrchestrator distributorUpdateOrchestrator) {
		this.distributorWriteRepository = distributorWriteRepository;
		this.distributorUpdateOrchestrator = distributorUpdateOrchestrator;
	}

	public Map<String, Object> batch(
			long companyId,
			Map<String, Object> user,
			List<Long> distributorIds,
			String isValidRaw,
			String requestLangTag) {
		String target = normalizeTargetIsValid(isValidRaw);
		List<Long> ids = normalizeIds(distributorIds);
		if (ids.isEmpty()) {
			throw new BadRequestException("请选择店铺");
		}
		if (ids.size() > MAX_BATCH_SIZE) {
			throw new BadRequestException("单次最多选择" + MAX_BATCH_SIZE + "个店铺");
		}

		List<Long> success = new ArrayList<>();
		List<Long> skipped = new ArrayList<>();
		List<Map<String, Object>> failed = new ArrayList<>();

		Map<String, Object> userCtx = user == null ? Map.of() : user;
		for (Long id : ids) {
			Optional<Distributor> opt =
					distributorWriteRepository.selectByCompanyAndIdIncludeDeleted(companyId, id);
			if (opt.isEmpty()) {
				failed.add(fail(id, "店铺不存在"));
				continue;
			}
			Distributor row = opt.get();
			String current = normalizeStoredIsValid(row.getIsValid());
			if ("delete".equals(current)) {
				skipped.add(id);
				continue;
			}
			if (row.getDistributorSelf() != null && row.getDistributorSelf() == 1) {
				skipped.add(id);
				continue;
			}
			if (target.equals(current)) {
				skipped.add(id);
				continue;
			}
			try {
				Map<String, Object> merged = new LinkedHashMap<>();
				merged.put("company_id", companyId);
				merged.put("distributor_id", id);
				merged.put("is_valid", target);
				distributorUpdateOrchestrator.update(
						merged, userCtx, id, requestLangTag, Set.of());
				success.add(id);
			} catch (BadRequestException | ResourceException e) {
				failed.add(fail(id, e.getMessage()));
			} catch (RuntimeException e) {
				failed.add(fail(id, e.getMessage() == null ? "更新失败" : e.getMessage()));
			}
		}

		if (success.isEmpty() && skipped.isEmpty() && failed.isEmpty()) {
			throw new BadRequestException("请选择店铺");
		}
		if (success.isEmpty() && failed.isEmpty()) {
			throw new BadRequestException("无可操作店铺");
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("success", success);
		out.put("skipped", skipped);
		out.put("failed", failed);
		return out;
	}

	private static String normalizeTargetIsValid(String raw) {
		String s = raw == null ? "" : raw.trim();
		if (!ALLOWED_IS_VALID.contains(s)) {
			throw new BadRequestException("is_valid 必须是 true、false、closed 或 delete");
		}
		return s;
	}

	/** Align historical 1/0 with true/false for same-status skip. */
	private static String normalizeStoredIsValid(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		String s = raw.trim();
		if ("1".equals(s)) {
			return "true";
		}
		if ("0".equals(s)) {
			return "false";
		}
		return s;
	}

	private static List<Long> normalizeIds(List<Long> distributorIds) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<Long> uniq = new LinkedHashSet<>();
		for (Long id : distributorIds) {
			if (id != null && id > 0L) {
				uniq.add(id);
			}
		}
		return new ArrayList<>(uniq);
	}

	private static Map<String, Object> fail(long id, String reason) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("distributor_id", id);
		m.put("reason", reason == null || reason.isBlank() ? "失败" : reason);
		return m;
	}
}
