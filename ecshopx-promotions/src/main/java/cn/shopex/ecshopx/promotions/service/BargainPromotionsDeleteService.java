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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.BargainPromotions;
import cn.shopex.ecshopx.promotions.domain.UserBargains;
import cn.shopex.ecshopx.promotions.mapper.BargainPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.UserBargainsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Locale;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BargainPromotionsDeleteService {

	private final BargainPromotionsMapper bargainPromotionsMapper;
	private final UserBargainsMapper userBargainsMapper;
	private final MessageSource messageSource;

	public BargainPromotionsDeleteService(
			BargainPromotionsMapper bargainPromotionsMapper,
			UserBargainsMapper userBargainsMapper,
			MessageSource messageSource) {
		this.bargainPromotionsMapper = bargainPromotionsMapper;
		this.userBargainsMapper = userBargainsMapper;
		this.messageSource = messageSource;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteBargain(long companyId, long bargainId, Locale locale) {
		BargainPromotions entity = bargainPromotionsMapper.selectById(bargainId);
		if (entity == null) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_not_exist_with_id",
							new Object[] {bargainId},
							locale));
		}
		if (entity.getCompanyId() == null || !Objects.equals(entity.getCompanyId(), companyId)) {
			throw new ResourceException(
					messageSource.getMessage("promotions.bargain.delete_bargain_info_error", null, locale));
		}
		LambdaQueryWrapper<UserBargains> w = Wrappers.lambdaQuery();
		w.eq(UserBargains::getBargainId, bargainId);
		Long cnt = userBargainsMapper.selectCount(w);
		if (cnt != null && cnt > 0L) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_has_user_cannot_delete", null, locale));
		}
		int rows = bargainPromotionsMapper.deleteById(bargainId);
		if (rows == 0) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.bargain.bargain_activity_delete_not_exist_with_id",
							new Object[] {bargainId},
							locale));
		}
	}
}
