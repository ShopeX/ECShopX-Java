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

package cn.shopex.ecshopx.distribution.service.distributorvalid;

import cn.shopex.ecshopx.distribution.domain.DistributorWhiteList;
import cn.shopex.ecshopx.distribution.mapper.DistributorWhiteListMapper;
import cn.shopex.ecshopx.members.service.admin.MembersContactByUserIdsLookupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorIsValidWhiteListBranchService {

	private final DistributorWhiteListMapper distributorWhiteListMapper;
	private final MembersContactByUserIdsLookupService membersContactByUserIdsLookupService;
	private final DistributorIsValidCoreQueryService distributorIsValidCoreQueryService;
	private final DistributorIsValidNearShopService distributorIsValidNearShopService;

	public DistributorIsValidWhiteListBranchService(
			DistributorWhiteListMapper distributorWhiteListMapper,
			MembersContactByUserIdsLookupService membersContactByUserIdsLookupService,
			DistributorIsValidCoreQueryService distributorIsValidCoreQueryService,
			DistributorIsValidNearShopService distributorIsValidNearShopService) {
		this.distributorWhiteListMapper = distributorWhiteListMapper;
		this.membersContactByUserIdsLookupService = membersContactByUserIdsLookupService;
		this.distributorIsValidCoreQueryService = distributorIsValidCoreQueryService;
		this.distributorIsValidNearShopService = distributorIsValidNearShopService;
	}

	public Map<String, Object> getWhiteListDistributor(
			long userId, long companyId, String distributorIdRaw, String lngRaw, String latRaw) {
		List<DistributorWhiteList> allCompanyRows =
				distributorWhiteListMapper.selectList(
						new LambdaQueryWrapper<DistributorWhiteList>()
								.eq(DistributorWhiteList::getCompanyId, companyId)
								.orderByDesc(DistributorWhiteList::getCreated));
		if (allCompanyRows.isEmpty()) {
			return new LinkedHashMap<>();
		}
		LinkedHashSet<Long> distinctUserIds = new LinkedHashSet<>();
		if (userId > 0L) {
			distinctUserIds.add(userId);
		}
		if (distinctUserIds.isEmpty()) {
			return new LinkedHashMap<>();
		}
		List<Long> userIdList = new ArrayList<>(distinctUserIds);
		int limit = Math.max(1, userIdList.size());
		Map<Long, Map<String, String>> contacts =
				membersContactByUserIdsLookupService.loadDecryptedContactsByUserIds(userIdList, limit);
		Map<String, String> contactRow = contacts.get(userId);
		String mobileStored = contactRow == null ? null : contactRow.get("mobile");
		if (!StringUtils.hasText(mobileStored)) {
			return new LinkedHashMap<>();
		}
		List<DistributorWhiteList> rows =
				allCompanyRows.stream()
						.filter(w -> mobileStored.equals(w.getMobile()))
						.collect(Collectors.toList());
		if (rows.isEmpty()) {
			return new LinkedHashMap<>();
		}
		List<Long> distributorIdList =
				rows.stream().map(DistributorWhiteList::getDistributorId).distinct().collect(Collectors.toList());
		if (whiteListSpecifiedDistributorId(distributorIdRaw)) {
			long did = parseWhitelistDistributorId(distributorIdRaw);
			if (!distributorIdList.contains(did)) {
				return new LinkedHashMap<>();
			}
			Map<String, Object> f = new LinkedHashMap<>();
			f.put("company_id", companyId);
			f.put("distributor_id", Long.valueOf(did));
			f.put("open_divided", Integer.valueOf(1));
			return distributorIsValidCoreQueryService.getInfo(f);
		}
		if (coordinatesLookPresent(lngRaw, latRaw)) {
			double lat = Double.parseDouble(latRaw.trim());
			double lng = Double.parseDouble(lngRaw.trim());
			Map<String, Object> f = new LinkedHashMap<>();
			f.put("company_id", companyId);
			f.put("distributor_id", new ArrayList<>(distributorIdList));
			f.put("open_divided", Integer.valueOf(1));
			Map<String, Object> tmp = distributorIsValidNearShopService.getNearShopData(f, lat, lng, 0);
			Object rd = tmp.get("real_default");
			if (rd instanceof Number n && n.intValue() == 1) {
				return new LinkedHashMap<>();
			}
			if (Boolean.TRUE.equals(rd)) {
				return new LinkedHashMap<>();
			}
			return tmp;
		}
		if (parseWhitelistDistributorId(distributorIdRaw) == 0L) {
			return distributorIsValidCoreQueryService.getInfoForNewestOpenDividedWhitelistDistributor(
					companyId, distributorIdList);
		}
		return new LinkedHashMap<>();
	}

	private static boolean whiteListSpecifiedDistributorId(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return false;
		}
		String t = raw.trim();
		if ("0".equals(t)) {
			return false;
		}
		return !"false".equalsIgnoreCase(t);
	}

	private static long parseWhitelistDistributorId(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (t.isEmpty() || "false".equalsIgnoreCase(t)) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean coordinatesLookPresent(String lng, String lat) {
		if (lng == null || lat == null) {
			return false;
		}
		String lt = lng.trim();
		String la = lat.trim();
		if (lt.isEmpty() || la.isEmpty()) {
			return false;
		}
		double ln;
		double laNum;
		try {
			ln = Double.parseDouble(lt);
			laNum = Double.parseDouble(la);
		} catch (NumberFormatException e) {
			return false;
		}
		if (!Double.isFinite(ln) || !Double.isFinite(laNum)) {
			return false;
		}
		return ln != 0d && laNum != 0d;
	}
}
