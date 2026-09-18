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

package cn.shopex.ecshopx.companys.repository;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.deliverystaff.AdminDeliveryStaffDataExportFilter;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class OperatorsDeliveryStaffDataSupportRepository {

	private final OperatorsMapper operatorsMapper;

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public OperatorsDeliveryStaffDataSupportRepository(
			OperatorsMapper operatorsMapper, SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.operatorsMapper = operatorsMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	public long count(AdminDeliveryStaffDataExportFilter filter) {
		return operatorsMapper.countSelfDeliveryStaffForDeliveryStaffData(
				filter.getCompanyId(),
				blankToNull(filter.getUsername()),
				filter.getMerchantIdForOperators(),
				resolveMobileEncrypted(filter.getMobilePlain()),
				filter.getMatchDistributorIds());
	}

	public List<Operators> page(AdminDeliveryStaffDataExportFilter filter, int page, int pageSize) {
		int offset = Math.max(0, (page - 1) * pageSize);
		return operatorsMapper.pageSelfDeliveryStaffForDeliveryStaffData(
				filter.getCompanyId(),
				blankToNull(filter.getUsername()),
				filter.getMerchantIdForOperators(),
				resolveMobileEncrypted(filter.getMobilePlain()),
				filter.getMatchDistributorIds(),
				offset,
				pageSize);
	}

	private static String blankToNull(String s) {
		return StringUtils.hasText(s) ? s.trim() : null;
	}

	private String resolveMobileEncrypted(String plain) {
		if (!StringUtils.hasText(plain)) {
			return null;
		}
		return sensitiveFieldEncryptor.encrypt(plain.trim());
	}
}
