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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.excard.ExcardInventoryPort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountLogs;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountLogsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.service.excard.ExcardNormalOrderCreateFacadeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserDiscountConsumeExCardTransactionService {

	private final UserDiscountMapper userDiscountMapper;
	private final UserDiscountLogsMapper userDiscountLogsMapper;
	private final UserDiscountVerifyCodeService verifyCodeService;
	private final ExcardInventoryPort excardInventoryPort;
	private final MemberAccountService memberAccountService;
	private final ExcardNormalOrderCreateFacadeService excardNormalOrderCreateFacadeService;
	private final DistributorMapper distributorMapper;

	public UserDiscountConsumeExCardTransactionService(
			UserDiscountMapper userDiscountMapper,
			UserDiscountLogsMapper userDiscountLogsMapper,
			UserDiscountVerifyCodeService verifyCodeService,
			ExcardInventoryPort excardInventoryPort,
			MemberAccountService memberAccountService,
			ExcardNormalOrderCreateFacadeService excardNormalOrderCreateFacadeService,
			DistributorMapper distributorMapper) {
		this.userDiscountMapper = userDiscountMapper;
		this.userDiscountLogsMapper = userDiscountLogsMapper;
		this.verifyCodeService = verifyCodeService;
		this.excardInventoryPort = excardInventoryPort;
		this.memberAccountService = memberAccountService;
		this.excardNormalOrderCreateFacadeService = excardNormalOrderCreateFacadeService;
		this.distributorMapper = distributorMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> runInTransaction(
			long companyId,
			long userCardId,
			String verifySegment,
			Object distributorIdParam,
			long distributorIdForOrder) {
		UserDiscount userCard = userDiscountMapper.selectOne(new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getId, userCardId)
				.eq(UserDiscount::getCompanyId, companyId)
				.last("LIMIT 1"));
		if (userCard == null) {
			throw new ResourceException("兑换券不存在");
		}
		if (userCard.getStatus() == null || userCard.getStatus() != 10) {
			throw new ResourceException("兑换券状态错误");
		}
		if (!"new_gift".equals(userCard.getCardType())) {
			throw new ResourceException("兑换券类型错误");
		}
		String expected = verifyCodeService.crcCode10(nullToEmpty(userCard.getCode())
				+ nullToEmpty(userCard.getRelDistributorIds())
				+ nullToEmpty(userCard.getRelItemIds()));
		if (!expected.equals(verifySegment)) {
			throw new ResourceException("兑换码错误");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (userCard.getBeginDate() != null && userCard.getBeginDate() > now) {
			throw new ResourceException("兑换券核销失败，该兑换券未到使用日期");
		}
		if (userCard.getEndDate() != null && userCard.getEndDate() <= now) {
			throw new ResourceException("兑换券核销失败，该兑换券已过期");
		}
		String itemIdStr = nullToEmpty(userCard.getRelItemIds()).trim();
		String distributorIdStr = nullToEmpty(userCard.getRelDistributorIds()).trim();
		if (!distributorMatches(distributorIdParam, distributorIdStr)) {
			throw new ResourceException("兑换券核销失败，店铺错误");
		}
		long itemId;
		try {
			itemId = Long.parseLong(itemIdStr);
		} catch (NumberFormatException e) {
			throw new ResourceException("兑换券不存在");
		}
		boolean isTotalStore = excardInventoryPort.resolveIsTotalStore(companyId, itemId, distributorIdForOrder);
		boolean unlocked = excardInventoryPort.minusItemStore(companyId, itemId, -1, distributorIdForOrder, isTotalStore);
		if (!unlocked) {
			throw new ResourceException("兑换券核销失败，请稍后重试");
		}
		Map<String, Object> memberInfo = Map.of();
		if (userCard.getUserId() != null && userCard.getUserId() > 0L) {
			memberInfo = memberAccountService.getMemberInfo(userCard.getUserId(), companyId);
		}
		String mobile = stringVal(memberInfo.get("mobile"));
		if (!StringUtils.hasText(mobile)) {
			throw new ResourceException("未授权手机号，请授权");
		}
		Map<String, Object> orderParams = new LinkedHashMap<>();
		List<Map<String, Object>> items = new ArrayList<>();
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("item_id", itemId);
		line.put("num", 1);
		items.add(line);
		orderParams.put("items", items);
		orderParams.put("company_id", companyId);
		orderParams.put("user_id", userCard.getUserId());
		orderParams.put("mobile", mobile);
		orderParams.put("receipt_type", "");
		orderParams.put("pay_type", "");
		orderParams.put("order_type", "normal");
		orderParams.put("user_card_id", userCard.getId());
		orderParams.put("distributor_id", distributorIdForOrder);
		Map<String, Object> orderData = excardNormalOrderCreateFacadeService.createThenComplete(orderParams);
		userDiscountMapper.update(
				null,
				new LambdaUpdateWrapper<UserDiscount>()
						.eq(UserDiscount::getId, userCardId)
						.eq(UserDiscount::getCompanyId, companyId)
						.set(UserDiscount::getStatus, 2));
		String shopName = "微商城";
		Distributor shop = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getDistributorId, distributorIdForOrder)
				.last("LIMIT 1"));
		if (shop != null && StringUtils.hasText(shop.getName())) {
			shopName = shop.getName();
		}
		UserDiscountLogs logRow = new UserDiscountLogs();
		logRow.setUserId(userCard.getUserId());
		logRow.setCompanyId(companyId);
		logRow.setMobile(mobile);
		logRow.setUsername(stringVal(memberInfo.get("username")));
		logRow.setCardId(userCard.getCardId());
		logRow.setCode(userCard.getCode());
		logRow.setTitle(userCard.getTitle());
		logRow.setCardType(userCard.getCardType());
		logRow.setShopName(shopName);
		logRow.setUsedTime(now);
		logRow.setUsedStatus("consume");
		logRow.setUsedOrder(String.valueOf(orderData.get("order_id")));
		userDiscountLogsMapper.insert(logRow);
		return orderData;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static boolean distributorMatches(Object requestDid, String cardRelRaw) {
		String c = cardRelRaw == null ? "" : cardRelRaw.trim();
		String r = requestDid == null ? "" : requestDid.toString().trim();
		try {
			long cl = Long.parseLong(c);
			long rl = Long.parseLong(r);
			return cl == rl;
		} catch (NumberFormatException e) {
			return r.equals(c);
		}
	}
}
