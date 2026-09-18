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

package cn.shopex.ecshopx.members.service.h5.bind;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmUpdateMemberInfoByMobilePort;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import cn.shopex.ecshopx.workwechat.domain.WorkWechatRel;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Synchronous marketing-center bind flow shared by wxapp async callback and queued member-register job.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberSalespersonBindMarketingCoordinator {

	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;
	private final WorkWechatRelMapper workWechatRelMapper;
	private final ShoppingGuideForH5BindLookup shoppingGuideForH5BindLookup;
	private final DmCrmSettingReadPort dmCrmSettingReadPort;
	private final DmCrmUpdateMemberInfoByMobilePort dmCrmUpdateMemberInfoByMobilePort;
	private final MemberAccountService memberAccountService;

	public void executeSalespersonBindForMember(
			long companyId, String unionid, String workUserid, int customerType, String mobile, long userId) {
		if (companyId <= 0L || !StringUtils.hasText(unionid) || !StringUtils.hasText(workUserid) || customerType <= 0) {
			return;
		}
		try {
			Map<String, Object> dataPayload = new LinkedHashMap<>();
			dataPayload.put("company_id", String.valueOf(companyId));
			dataPayload.put("unionid", unionid);
			dataPayload.put("gu_user_id", workUserid);
			dataPayload.put("customer_type", String.valueOf(customerType));

			Map<String, Object> root = marketingCenterOpenApiSignedFormClient.postReturningFullRootMap(
					companyId, "salesperson.bind.member", dataPayload);
			int code;
			try {
				code = Integer.parseInt(String.valueOf(root.getOrDefault("code", 0)));
			} catch (NumberFormatException e) {
				code = 0;
			}

			if (code != 200) {
				rollbackBindIfNeeded(companyId, userId, workUserid);
			} else if (dmCrmSettingReadPort.isPointIntegrationOpen(companyId)) {
				afterBindMemberSuccessSyncCrmAndRelShop(
						companyId, unionid, workUserid, customerType, mobile, userId);
			}
		} catch (Exception e) {
			log.warn("salesperson bind marketing failed companyId={} userId={}: {}", companyId, userId, e.toString());
		}
	}

	/**
	 * Runs after a successful {@code salesperson.bind.member} call when company CRM integration is on: posts
	 * {@code basics.salesperson.relShop}, then updates CRM member fields (including mobile-sourced profile data).
	 */
	private void afterBindMemberSuccessSyncCrmAndRelShop(
			long companyId,
			String unionid,
			String workUserid,
			int customerType,
			String mobilePlain,
			long userId) {
		try {
			Map<String, Object> relShopPayload = new LinkedHashMap<>();
			relShopPayload.put("company_id", String.valueOf(companyId));
			relShopPayload.put("unionid", unionid);
			relShopPayload.put("gu_user_id", workUserid);
			relShopPayload.put("customer_type", String.valueOf(customerType));
			relShopPayload.put("user_id", String.valueOf(userId));
			marketingCenterOpenApiSignedFormClient.postReturningFullRootMap(
					companyId, "basics.salesperson.relShop", relShopPayload);

			Map<String, Object> memberRow = memberAccountService.getMemberInfo(userId, companyId);
			LinkedHashMap<String, Object> dm = new LinkedHashMap<>();
			if (memberRow != null && !memberRow.isEmpty()) {
				dm.put(
						"mobile",
						StringUtils.hasText(mobilePlain)
								? mobilePlain.trim()
								: stringVal(memberRow.get("mobile")).trim());
				dm.put("name", memberRow.get("username"));
				dm.put("sex", memberRow.get("sex"));
				dm.put("birthday", memberRow.get("birthday"));
				dm.put("email", memberRow.get("email"));
			} else {
				dm.put("mobile", mobilePlain != null ? mobilePlain.trim() : "");
			}
			dmCrmUpdateMemberInfoByMobilePort.updateMemberInfoByMobile(companyId, dm);
		} catch (Exception e) {
			log.warn(
					"salesperson bind post-success crm/relShop sync failed companyId={} userId={}: {}",
					companyId,
					userId,
					e.toString());
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private void rollbackBindIfNeeded(long companyId, long userId, String workUserid) {
		try {
			Map<String, Object> guide = shoppingGuideForH5BindLookup.getShoppingGuideDetailForH5Bind(companyId, workUserid);
			if (guide == null || guide.isEmpty()) {
				return;
			}
			Object sidObj = guide.get("salesperson_id");
			if (sidObj == null) {
				return;
			}
			long salespersonId;
			try {
				salespersonId = Long.parseLong(String.valueOf(sidObj).trim());
			} catch (NumberFormatException e) {
				return;
			}
			if (salespersonId <= 0L) {
				return;
			}
			WorkWechatRel isBound = workWechatRelMapper.selectOne(new LambdaQueryWrapper<WorkWechatRel>()
					.eq(WorkWechatRel::getUserId, userId)
					.eq(WorkWechatRel::getCompanyId, companyId)
					.eq(WorkWechatRel::getSalespersonId, salespersonId)
					.last("LIMIT 1"));
			if (isBound == null) {
				return;
			}
			LambdaUpdateWrapper<WorkWechatRel> uw = new LambdaUpdateWrapper<>();
			uw.eq(WorkWechatRel::getUserId, userId)
					.eq(WorkWechatRel::getCompanyId, companyId)
					.eq(WorkWechatRel::getSalespersonId, salespersonId)
					.set(WorkWechatRel::getIsBind, false);
			workWechatRelMapper.update(null, uw);
		} catch (Exception e) {
			log.warn("salesperson bind rollback failed companyId={} userId={}: {}", companyId, userId, e.toString());
		}
	}
}
