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

package cn.shopex.ecshopx.distribution.service.hfpay;

import cn.shopex.ecshopx.common.hfpay.HfpayEnterapplyOpenSplitPort;
import cn.shopex.ecshopx.distribution.service.DistributorUpdateOrchestrator;
import cn.shopex.ecshopx.distribution.service.HfpayLedgerConfigReadService;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplyReadService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HfpayEnterapplyOpenSplitService implements HfpayEnterapplyOpenSplitPort {

	private final HfpayLedgerConfigReadService hfpayLedgerConfigReadService;
	private final HfpayEnterapplyReadService hfpayEnterapplyReadService;
	private final HfpayEnterapplyMapper hfpayEnterapplyMapper;
	private final DistributorUpdateOrchestrator distributorUpdateOrchestrator;

	public HfpayEnterapplyOpenSplitService(
			HfpayLedgerConfigReadService hfpayLedgerConfigReadService,
			HfpayEnterapplyReadService hfpayEnterapplyReadService,
			HfpayEnterapplyMapper hfpayEnterapplyMapper,
			DistributorUpdateOrchestrator distributorUpdateOrchestrator) {
		this.hfpayLedgerConfigReadService = hfpayLedgerConfigReadService;
		this.hfpayEnterapplyReadService = hfpayEnterapplyReadService;
		this.hfpayEnterapplyMapper = hfpayEnterapplyMapper;
		this.distributorUpdateOrchestrator = distributorUpdateOrchestrator;
	}

	@Override
	public Map<String, Object> openSplit(
			long companyId,
			Map<String, Object> mergedParams,
			Map<String, Object> operatorUser,
			String requestLangTag) {
		hfpayLedgerConfigReadService.assertLedgerOpenForShopOpenSplit(companyId);
		long distributorId = parseDistributorIdFromMerged(mergedParams);
		Map<String, Object> enterapplyData = hfpayEnterapplyReadService.getEnterapply(companyId, distributorId);

		boolean requestIsOpenTrue = isOpenTrueForInitApply(mergedParams.get("is_open"));
		if (enterapplyData == null && requestIsOpenTrue) {
			HfpayEnterapply entity = new HfpayEnterapply();
			entity.setCompanyId(companyId);
			entity.setDistributorId(distributorId);
			entity.setApplyType("1");
			entity.setStatus("1");
			hfpayEnterapplyMapper.insert(entity);
		}

		String patchIsOpen = patchIsOpenString(mergedParams.get("is_open"));
		Map<String, Object> mergedForUpdate = new LinkedHashMap<>();
		mergedForUpdate.put("company_id", companyId);
		if (patchIsOpen != null) {
			mergedForUpdate.put("is_open", patchIsOpen);
		}

		return distributorUpdateOrchestrator.update(
				mergedForUpdate, operatorUser, distributorId, requestLangTag, null);
	}

	/** Only string {@code "true"} / {@code "false"} (case-sensitive) are written to the shop patch; booleans are ignored. */
	private static String patchIsOpenString(Object raw) {
		if (raw instanceof String s) {
			if ("true".equals(s)) {
				return "true";
			}
			if ("false".equals(s)) {
				return "false";
			}
		}
		return null;
	}

	/** Init apply insert only when {@code params['is_open']} is the string {@code "true"} (strict string compare). */
	private static boolean isOpenTrueForInitApply(Object raw) {
		return raw instanceof String s && "true".equals(s);
	}

	private static long parseDistributorIdFromMerged(Map<String, Object> mergedParams) {
		Object raw = mergedParams.get("distributor_id");
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
