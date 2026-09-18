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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Advertisement;
import cn.shopex.ecshopx.distribution.mapper.AdvertisementMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class ShopScreenAdvertisementDeleteService {

	private final AdvertisementMapper advertisementMapper;

	private final MessageSource messageSource;

	public ShopScreenAdvertisementDeleteService(
			AdvertisementMapper advertisementMapper, MessageSource messageSource) {
		this.advertisementMapper = advertisementMapper;
		this.messageSource = messageSource;
	}

	public void deleteAdvertisement(long companyId, long advertisementId) {
		Locale locale = LocaleContextHolder.getLocale();
		int affected =
				advertisementMapper.delete(
						Wrappers.<Advertisement>lambdaQuery()
								.eq(Advertisement::getCompanyId, companyId)
								.eq(Advertisement::getId, advertisementId));
		if (affected == 0) {
			throw new ResourceException(
					messageSource.getMessage(
							"distribution.shop_screen.advertisement.delete_not_exist", null, locale));
		}
	}
}
