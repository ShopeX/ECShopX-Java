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

package cn.shopex.ecshopx.kaquan.service.cardpackage;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.CardPackage;
import cn.shopex.ecshopx.kaquan.mapper.CardPackageMapper;
import cn.shopex.ecshopx.kaquan.service.discount.KaquanDiscountCardMessages;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardPackageEditService {

	private final CardPackageMapper cardPackageMapper;
	private final CardPackageMultiLangWriteService cardPackageMultiLangWriteService;

	public CardPackageEditService(CardPackageMapper cardPackageMapper,
			CardPackageMultiLangWriteService cardPackageMultiLangWriteService) {
		this.cardPackageMapper = cardPackageMapper;
		this.cardPackageMultiLangWriteService = cardPackageMultiLangWriteService;
	}

	public void editPackage(long companyId, Map<String, Object> inputData) {
		long packageId = parsePackageIdOrThrow(inputData.get("package_id"));

		Object titleRaw = inputData.get("title");
		if (!(titleRaw instanceof String)) {
			throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_TITLE_REQUIRED);
		}
		String title = ((String) titleRaw).trim();
		if (title.isEmpty() || title.length() > 10) {
			throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_TITLE_REQUIRED);
		}

		if (inputData.containsKey("package_describe") && inputData.get("package_describe") != null) {
			Object desc = inputData.get("package_describe");
			if (!(desc instanceof String) || ((String) desc).length() > 20) {
				throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_DESCRIBE_MAX);
			}
		}

		String packageDescribe = Objects.toString(inputData.get("package_describe"), "");

		LambdaQueryWrapper<CardPackage> q = new LambdaQueryWrapper<>();
		q.eq(CardPackage::getCompanyId, companyId)
				.eq(CardPackage::getPackageId, packageId)
				.eq(CardPackage::getRowStatus, 1);
		CardPackage existing = cardPackageMapper.selectOne(q);
		if (existing == null) {
			throw new ResourceException(KaquanDiscountCardMessages.PACKAGE_NOT_FOUND);
		}

		CardPackage patch = new CardPackage();
		patch.setTitle(title);
		patch.setPackageDescribe(packageDescribe);

		LambdaUpdateWrapper<CardPackage> u = new LambdaUpdateWrapper<>();
		u.eq(CardPackage::getCompanyId, companyId)
				.eq(CardPackage::getPackageId, packageId)
				.eq(CardPackage::getRowStatus, 1);

		int rows = cardPackageMapper.update(patch, u);
		if (rows == 0) {
			throw new ResourceException(KaquanDiscountCardMessages.PACKAGE_NOT_FOUND);
		}

		cardPackageMultiLangWriteService.syncFieldsAfterUpdate(packageId, companyId, title, packageDescribe);
	}

	@Transactional(rollbackFor = Exception.class)
	public void incrCardPackageGetNum(long companyId, long packageId) {
		int now = (int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE);
		LambdaUpdateWrapper<CardPackage> u = new LambdaUpdateWrapper<>();
		u.eq(CardPackage::getCompanyId, companyId)
				.eq(CardPackage::getPackageId, packageId)
				.eq(CardPackage::getRowStatus, 1)
				.setSql("get_num = IFNULL(get_num,0) + 1")
				.set(CardPackage::getUpdated, now);
		cardPackageMapper.update(null, u);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deletePackage(long companyId, long packageId) {
		CardPackage patch = new CardPackage();
		patch.setRowStatus(0);
		LambdaUpdateWrapper<CardPackage> u = new LambdaUpdateWrapper<>();
		u.eq(CardPackage::getPackageId, packageId).eq(CardPackage::getCompanyId, companyId);
		cardPackageMapper.update(patch, u);
	}

	private static long parsePackageIdOrThrow(Object raw) {
		if (raw == null) {
			throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_ID_REQUIRED);
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v < 1L) {
				throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_ID_REQUIRED);
			}
			return v;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_ID_REQUIRED);
			}
			try {
				long v = Long.parseLong(t);
				if (v < 1L) {
					throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_ID_REQUIRED);
				}
				return v;
			} catch (NumberFormatException e) {
				throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_ID_REQUIRED);
			}
		}
		throw new BadRequestException(KaquanDiscountCardMessages.PACKAGE_ID_REQUIRED);
	}
}
