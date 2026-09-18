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
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserDiscountExchangeCardInfoService {

	private final UserDiscountMapper userDiscountMapper;
	private final UserDiscountVerifyCodeService verifyCodeService;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final ExchangeCardBarcodeImageService exchangeCardBarcodeImageService;

	public UserDiscountExchangeCardInfoService(
			UserDiscountMapper userDiscountMapper,
			UserDiscountVerifyCodeService verifyCodeService,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			ExchangeCardBarcodeImageService exchangeCardBarcodeImageService) {
		this.userDiscountMapper = userDiscountMapper;
		this.verifyCodeService = verifyCodeService;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.exchangeCardBarcodeImageService = exchangeCardBarcodeImageService;
	}

	public Map<String, Object> buildExchangeCardInfo(long companyId, long userCardId, long userId) {
		UserDiscount userCard = userDiscountMapper.selectOne(new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getId, userCardId)
				.eq(UserDiscount::getCompanyId, companyId)
				.eq(UserDiscount::getUserId, userId)
				.last("LIMIT 1"));
		if (userCard == null) {
			throw new ResourceException("兑换券不存在");
		}
		if (!"new_gift".equals(userCard.getCardType())) {
			throw new ResourceException("兑换券类型错误");
		}
		if (userCard.getStatus() == null || userCard.getStatus() != 10) {
			throw new ResourceException("兑换券状态错误");
		}
		Object distributorInfo = distributorRepositoryGetInfoSimpleService.getInfoSimple(
				companyId, nullToEmpty(userCard.getRelDistributorIds()));
		String verifyCode = verifyCodeService.crcCode10(nullToEmpty(userCard.getCode())
				+ nullToEmpty(userCard.getRelDistributorIds())
				+ nullToEmpty(userCard.getRelItemIds()));
		String barcodePayload = "excode:" + userCardId + "-" + verifyCode;
		byte[] barPng = exchangeCardBarcodeImageService.barcodePngBytes(barcodePayload);
		byte[] qrPng = exchangeCardBarcodeImageService.qrcodePngBytes(barcodePayload);
		String barB64 = Base64.getEncoder().encodeToString(barPng);
		String qrB64 = Base64.getEncoder().encodeToString(qrPng);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("distributor_info", distributorInfo);
		out.put("code", verifyCode);
		out.put("barcode_url", "data:image/jpg;base64," + barB64);
		out.put("qrcode_url", "data:image/jpg;base64," + qrB64);
		return out;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
