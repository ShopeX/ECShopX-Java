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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.wechat.WorkWechatSalespersonSyncPort;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class WorkWechatSalespersonSyncService implements WorkWechatSalespersonSyncPort {

	private final DistributorWriteRepository distributorWriteRepository;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final TransactionTemplate transactionTemplate;

	public WorkWechatSalespersonSyncService(
			DistributorWriteRepository distributorWriteRepository,
			ShopSalespersonMapper shopSalespersonMapper,
			ShopsRelSalespersonMapper shopsRelSalespersonMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			TransactionTemplate transactionTemplate) {
		this.distributorWriteRepository = distributorWriteRepository;
		this.shopSalespersonMapper = shopSalespersonMapper;
		this.shopsRelSalespersonMapper = shopsRelSalespersonMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public Map<String, Object> syncUserToSalesperson(long companyId, List<Map<String, Object>> userData) {
		List<Map<String, Object>> updateResults = new ArrayList<>();
		List<Map<String, Object>> createResults = new ArrayList<>();
		if (userData == null) {
			return buildOuter(updateResults, createResults);
		}
		for (Map<String, Object> v : userData) {
			if (v == null) {
				continue;
			}
			Object mobileObj = v.get("mobile");
			String mobilePlain = mobileObj == null ? "" : String.valueOf(mobileObj).trim();
			if (!StringUtils.hasText(mobilePlain)) {
				continue;
			}
			String nameForMsg = v.get("name") == null ? "" : String.valueOf(v.get("name"));
			String encMobile = sensitiveFieldEncryptor.encrypt(mobilePlain);
			ShopSalesperson existing = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
					.eq(ShopSalesperson::getCompanyId, companyId)
					.eq(ShopSalesperson::getMobile, encMobile)
					.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
					.last("LIMIT 1"));

			Integer mainDeptBoxed = parseIntegerOrNull(v.get("main_department"));
			int mainDeptId = mainDeptBoxed == null ? 0 : mainDeptBoxed;
			var distributorOpt = distributorWriteRepository.selectByCompanyIdAndWechatWorkDepartmentId(companyId, mainDeptId);
			if (distributorOpt.isEmpty()) {
				throw new ResourceException("企微成员" + nameForMsg + "所属主部门尚未绑定店铺，无法同步");
			}
			long distributorId = distributorOpt.get().getDistributorId();

			Object st = v.get("status");
			String isValid = Integer.valueOf(2).equals(parseIntegerOrNull(st)) ? "delete" : "true";
			String namePlain = v.get("name") == null ? "" : String.valueOf(v.get("name")).trim();
			Object uidObj = v.get("userid");
			String workUserid = uidObj == null ? "" : String.valueOf(uidObj);

			if (existing != null) {
				long sid = existing.getSalespersonId();
				List<Long> distributorIds = List.of(distributorId);
				transactionTemplate.executeWithoutResult(status -> {
					long now = Instant.now().getEpochSecond();
					LambdaUpdateWrapper<ShopSalesperson> uw = new LambdaUpdateWrapper<ShopSalesperson>()
							.eq(ShopSalesperson::getSalespersonId, sid)
							.eq(ShopSalesperson::getCompanyId, companyId)
							.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
							.set(ShopSalesperson::getIsValid, isValid)
							.set(ShopSalesperson::getName, sensitiveFieldEncryptor.encrypt(namePlain))
							.set(ShopSalesperson::getWorkUserid, workUserid)
							.set(ShopSalesperson::getSalespersonType, "shopping_guide")
							.set(ShopSalesperson::getCompanyId, companyId)
							.set(ShopSalesperson::getShopId, String.valueOf(distributorId))
							.set(ShopSalesperson::getUpdated, now);
					int rows = shopSalespersonMapper.update(null, uw);
					if (rows == 0) {
						throw new ResourceException("未查询到更新数据");
					}
					if (distributorIds != null && !distributorIds.isEmpty()) {
						shopsRelSalespersonMapper.delete(new LambdaQueryWrapper<ShopsRelSalesperson>()
								.eq(ShopsRelSalesperson::getCompanyId, companyId)
								.eq(ShopsRelSalesperson::getSalespersonId, sid));
						for (Long did : distributorIds) {
							if (did == null) {
								continue;
							}
							ShopsRelSalesperson rel = new ShopsRelSalesperson();
							rel.setShopId(did);
							rel.setSalespersonId(sid);
							rel.setCompanyId(companyId);
							rel.setStoreType("distributor");
							shopsRelSalespersonMapper.insert(rel);
						}
					}
				});
				ShopSalesperson refreshed = shopSalespersonMapper.selectById(sid);
				if (refreshed == null) {
					throw new ResourceException("更新的人员不存在");
				}
				updateResults.add(DistributorShoppingGuideAddService.buildMobileFindDataMap(refreshed, sensitiveFieldEncryptor));
			} else {
				ShopSalesperson otherType = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
						.eq(ShopSalesperson::getCompanyId, companyId)
						.eq(ShopSalesperson::getMobile, encMobile)
						.ne(ShopSalesperson::getSalespersonType, "shopping_guide")
						.last("LIMIT 1"));
				if (otherType != null) {
					String t = otherType.getSalespersonType();
					if ("admin".equals(t)) {
						throw new ResourceException("当前手机号已经已绑定为管理员");
					}
					if ("verification_clerk".equals(t)) {
						throw new ResourceException("当前手机号已经已绑定为核销员");
					}
					throw new ResourceException("当前手机号已经已绑定");
				}
				ShopSalesperson row = new ShopSalesperson();
				row.setName(sensitiveFieldEncryptor.encrypt(namePlain));
				row.setMobile(encMobile);
				row.setCompanyId(companyId);
				row.setUserId(0);
				row.setSalespersonType("shopping_guide");
				row.setNumber("");
				row.setRole("0");
				row.setEmployeeStatus(1);
				row.setShopId(String.valueOf(distributorId));
				row.setWorkUserid(workUserid);
				row.setIsValid(isValid);
				long epochSecond = Instant.now().getEpochSecond();
				row.setCreatedTime(String.valueOf(epochSecond));
				row.setCreated(epochSecond);
				row.setUpdated(epochSecond);

				transactionTemplate.executeWithoutResult(status -> {
					shopSalespersonMapper.insert(row);
					ShopsRelSalesperson rel = new ShopsRelSalesperson();
					rel.setShopId(distributorId);
					rel.setSalespersonId(row.getSalespersonId());
					rel.setCompanyId(companyId);
					rel.setStoreType("distributor");
					shopsRelSalespersonMapper.insert(rel);
				});
				ShopSalesperson inserted = shopSalespersonMapper.selectById(row.getSalespersonId());
				if (inserted == null) {
					throw new ResourceException("同步导购数据失败");
				}
				createResults.add(DistributorShoppingGuideAddService.buildMobileFindDataMap(inserted, sensitiveFieldEncryptor));
			}
		}
		return buildOuter(updateResults, createResults);
	}

	private static Map<String, Object> buildOuter(
			List<Map<String, Object>> updateResults, List<Map<String, Object>> createResults) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("update_salesperson_reulst", updateResults);
		out.put("create_salesperson_result", createResults);
		return out;
	}

	private static Integer parseIntegerOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
