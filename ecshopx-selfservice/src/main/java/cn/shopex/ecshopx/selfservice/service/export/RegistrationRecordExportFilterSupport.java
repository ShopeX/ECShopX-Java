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

package cn.shopex.ecshopx.selfservice.service.export;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationRecordExportFilterSupport {

	private final RegistrationRecordMapper registrationRecordMapper;

	public RegistrationRecordExportFilterSupport(RegistrationRecordMapper registrationRecordMapper) {
		this.registrationRecordMapper = registrationRecordMapper;
	}

	public LambdaQueryWrapper<RegistrationRecord> buildExportWrapper(
			long companyId,
			long activityId,
			String mobilePlain,
			Integer startCreatedInclusive,
			Integer endCreatedInclusive,
			SensitiveFieldEncryptor encryptor) {
		LambdaQueryWrapper<RegistrationRecord> w = new LambdaQueryWrapper<>();
		w.eq(RegistrationRecord::getCompanyId, companyId);
		w.eq(RegistrationRecord::getActivityId, activityId);
		if (StringUtils.hasText(mobilePlain)) {
			w.eq(RegistrationRecord::getMobile, encryptor.encrypt(mobilePlain.trim()));
		}
		if (startCreatedInclusive != null) {
			w.ge(RegistrationRecord::getCreated, startCreatedInclusive);
		}
		if (endCreatedInclusive != null) {
			w.le(RegistrationRecord::getCreated, endCreatedInclusive);
		}
		return w;
	}

	public long countForExport(
			long companyId,
			long activityId,
			String mobilePlain,
			Integer startCreatedInclusive,
			Integer endCreatedInclusive,
			SensitiveFieldEncryptor encryptor) {
		return registrationRecordMapper.selectCount(
				buildExportWrapper(companyId, activityId, mobilePlain, startCreatedInclusive, endCreatedInclusive, encryptor));
	}
}
