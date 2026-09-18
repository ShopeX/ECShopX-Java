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

import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserDiscountUserRemoveCardService {

	private final UserDiscountMapper userDiscountMapper;

	public UserDiscountUserRemoveCardService(UserDiscountMapper userDiscountMapper) {
		this.userDiscountMapper = userDiscountMapper;
	}

	/**
	 * Locates at most one user discount row for the given scope and deletes it.
	 *
	 * @param recordId when non-null, restricts by primary key; when null, id is not filtered
	 * @param code when non-null and non-blank, restricts by code column (exact match)
	 * @return true if a row was found and deleted
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean removeUserReceivedCard(long companyId, long userId, Long recordId, String code) {
		var wrapper = Wrappers.lambdaQuery(UserDiscount.class)
				.eq(UserDiscount::getCompanyId, companyId)
				.eq(UserDiscount::getUserId, userId);
		if (recordId != null) {
			wrapper.eq(UserDiscount::getId, recordId);
		}
		if (StringUtils.hasText(code)) {
			wrapper.eq(UserDiscount::getCode, code.trim());
		}
		UserDiscount row = userDiscountMapper.selectOne(wrapper);
		if (row == null) {
			return false;
		}
		int n = userDiscountMapper.deleteById(row.getId());
		return n > 0;
	}
}
