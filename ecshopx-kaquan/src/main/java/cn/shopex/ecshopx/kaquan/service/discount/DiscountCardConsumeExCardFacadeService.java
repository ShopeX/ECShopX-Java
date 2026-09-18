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
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DiscountCardConsumeExCardFacadeService {

	private final UserDiscountConsumeExCardTransactionService transactionService;
	private final UserDiscountMapper userDiscountMapper;
	private final DiscountCardInfoByIdForConsumeService discountCardInfoByIdForConsumeService;
	private final DiscountCardConsumeSoftFailDistributorListService softFailDistributorListService;

	public DiscountCardConsumeExCardFacadeService(
			UserDiscountConsumeExCardTransactionService transactionService,
			UserDiscountMapper userDiscountMapper,
			DiscountCardInfoByIdForConsumeService discountCardInfoByIdForConsumeService,
			DiscountCardConsumeSoftFailDistributorListService softFailDistributorListService) {
		this.transactionService = transactionService;
		this.userDiscountMapper = userDiscountMapper;
		this.discountCardInfoByIdForConsumeService = discountCardInfoByIdForConsumeService;
		this.softFailDistributorListService = softFailDistributorListService;
	}

	public Map<String, Object> consume(
			HttpServletRequest request,
			long companyId,
			long userCardId,
			String verifySegment,
			Object distributorIdParam,
			Map<String, Object> operatorJwt) {
		long distributorIdForOrder = parseDistributorIdLong(distributorIdParam);
		return transactionService.runInTransaction(
				companyId, userCardId, verifySegment, distributorIdParam, distributorIdForOrder);
	}

	@SuppressWarnings("unused")
	public Map<String, Object> buildSoftFailDistributors(
			HttpServletRequest request,
			long companyId,
			long userCardId,
			Map<String, Object> operatorJwt,
			ResourceException caught) {
		UserDiscount userCard = userDiscountMapper.selectOne(new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getId, userCardId)
				.eq(UserDiscount::getCompanyId, companyId)
				.last("LIMIT 1"));
		if (userCard == null || userCard.getStatus() == null || userCard.getStatus() != 10) {
			throw new ResourceException("核销码不存在或有误，请检查");
		}
		Map<String, Object> cardInfo =
				discountCardInfoByIdForConsumeService.load(companyId, userCard.getCardId());
		if (cardInfo.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("list", java.util.List.of());
			empty.put("total_count", 0);
			return empty;
		}
		return softFailDistributorListService.buildDistributorPayload(request, operatorJwt, cardInfo);
	}

	private static long parseDistributorIdLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
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
