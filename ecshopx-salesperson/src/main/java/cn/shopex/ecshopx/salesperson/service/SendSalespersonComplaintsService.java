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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.salesperson.domain.SalemanCustomerComplaint;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.domain.ShopsRelSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.SalemanCustomerComplaintMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopsRelSalespersonMapper;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SendSalespersonComplaintsService {

	private final WorkWechatRelMapper workWechatRelMapper;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final ShopsRelSalespersonMapper shopsRelSalespersonMapper;
	private final SalemanCustomerComplaintMapper salemanCustomerComplaintMapper;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> sendSalespersonComplaints(
			long userId,
			long companyId,
			String userName,
			String userMobile,
			String complaintsContent,
			String complaintsImages) {
		if (userId <= 0L || userId > (long) Integer.MAX_VALUE) {
			throw new BadRequestException("导购更新用户信息错误");
		}
		if (companyId <= 0L || companyId > (long) Integer.MAX_VALUE) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		int userIdInt = Math.toIntExact(userId);
		int companyIdInt = Math.toIntExact(companyId);

		WorkWechatRel rel = workWechatRelMapper.selectOne(new LambdaQueryWrapper<WorkWechatRel>()
				.eq(WorkWechatRel::getUserId, userId)
				.eq(WorkWechatRel::getCompanyId, companyId)
				.eq(WorkWechatRel::getIsBind, Boolean.TRUE)
				.last("LIMIT 1"));
		if (rel == null) {
			throw new ResourceException("获取导购员信息失败");
		}

		long spId = rel.getSalespersonId();
		ShopSalesperson sp = shopSalespersonMapper.selectOne(new LambdaQueryWrapper<ShopSalesperson>()
				.eq(ShopSalesperson::getSalespersonId, spId)
				.eq(ShopSalesperson::getCompanyId, companyId)
				.eq(ShopSalesperson::getSalespersonType, "shopping_guide")
				.eq(ShopSalesperson::getIsValid, "true")
				.last("LIMIT 1"));
		if (sp == null) {
			throw new ResourceException("获取导购员信息失败！");
		}

		List<ShopsRelSalesperson> rels = shopsRelSalespersonMapper.selectList(new LambdaQueryWrapper<ShopsRelSalesperson>()
				.eq(ShopsRelSalesperson::getCompanyId, companyId)
				.eq(ShopsRelSalesperson::getSalespersonId, spId)
				.orderByAsc(ShopsRelSalesperson::getShopId));

		List<Long> distributorIds = new ArrayList<>();
		for (ShopsRelSalesperson r : rels) {
			if ("distributor".equals(r.getStoreType()) && r.getShopId() != null) {
				distributorIds.add(r.getShopId());
			}
		}
		Long firstDist = distributorIds.isEmpty() ? null : distributorIds.get(0);

		String storeName = "";
		if (firstDist != null && firstDist > 0L) {
			List<Map<String, Object>> easy =
					distributorRepositoryGetInfoSimpleService.listEasylistsByDistributorIds(companyId, List.of(firstDist));
			for (Map<String, Object> row : easy) {
				if (row == null) {
					continue;
				}
				if (!distributorIdMatches(row.get("distributor_id"), firstDist)) {
					continue;
				}
				Object n = row.get("name");
				storeName = n != null ? String.valueOf(n) : "";
				break;
			}
		}

		int now = (int) Instant.now().getEpochSecond();
		SalemanCustomerComplaint entity = new SalemanCustomerComplaint();
		entity.setUserId(userIdInt);
		entity.setCompanyId(companyIdInt);
		entity.setUserName(userName);
		entity.setUserMobile(userMobile);
		entity.setComplaintsContent(complaintsContent);
		entity.setComplaintsImages(complaintsImages);
		entity.setReplyStatus(false);
		entity.setSalemanId(sp.getSalespersonId().intValue());
		entity.setDistributorId(firstDist != null ? firstDist : 0L);
		entity.setSalemanName(sp.getName() != null ? sensitiveFieldEncryptor.decrypt(sp.getName()) : "");
		entity.setSalemanMobile(sp.getMobile() != null ? sensitiveFieldEncryptor.decrypt(sp.getMobile()) : "");
		entity.setSalemanAvatar(sp.getAvatar() != null ? sp.getAvatar() : "");
		entity.setSalemanDistributionName(storeName);
		entity.setCreated(now);
		entity.setUpdated(now);

		int n = salemanCustomerComplaintMapper.insert(entity);
		if (n <= 0 || entity.getId() == null) {
			throw new ResourceException("投诉失败,请稍后重试");
		}

		SalemanCustomerComplaint inserted = salemanCustomerComplaintMapper.selectById(entity.getId());
		if (inserted == null) {
			throw new ResourceException("投诉失败,请稍后重试");
		}
		return SalemanCustomerComplaintSwgRowConverter.toRow(inserted);
	}

	private static boolean distributorIdMatches(Object raw, long expected) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number num) {
			return num.longValue() == expected;
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim()) == expected;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
