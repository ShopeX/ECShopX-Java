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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.RegistrationRecordUpdateDataInfoCommand;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationRecordOutsideMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class RegistrationRecordUpdateDataInfoService {

	private final RegistrationRecordMapper registrationRecordMapper;
	private final RegistrationRecordOutsideMultiLangWriteService registrationRecordOutsideMultiLangWriteService;
	private final MessageSource messageSource;
	private final RegistrationRecordRowMapAssembler rowMapAssembler;

	public RegistrationRecordUpdateDataInfoService(
			RegistrationRecordMapper registrationRecordMapper,
			RegistrationRecordOutsideMultiLangWriteService registrationRecordOutsideMultiLangWriteService,
			MessageSource messageSource,
			RegistrationRecordRowMapAssembler rowMapAssembler) {
		this.registrationRecordMapper = registrationRecordMapper;
		this.registrationRecordOutsideMultiLangWriteService = registrationRecordOutsideMultiLangWriteService;
		this.messageSource = messageSource;
		this.rowMapAssembler = rowMapAssembler;
	}

	public Map<String, Object> updateDataInfo(
			long companyId, RegistrationRecordUpdateDataInfoCommand cmd, String requestLangTag, Locale locale) {
		LambdaQueryWrapper<RegistrationRecord> w =
				new LambdaQueryWrapper<RegistrationRecord>()
						.eq(RegistrationRecord::getCompanyId, companyId)
						.eq(RegistrationRecord::getRecordId, cmd.recordId());
		RegistrationRecord entity = registrationRecordMapper.selectOne(w);
		if (entity == null) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.no_update_data_found", null, locale));
		}
		int now = (int) java.time.Instant.now().getEpochSecond();
		entity.setUpdated(now);
		if (cmd.writeRemark()) {
			entity.setRemark(cmd.remarkValue());
		}
		int rows = registrationRecordMapper.updateById(entity);
		if (rows == 0) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_record.no_update_data_found", null, locale));
		}

		Map<String, Object> langPayload = new LinkedHashMap<>();
		if (cmd.writeRemark()) {
			langPayload.put("remark", cmd.remarkValue());
		}
		if (!langPayload.isEmpty()) {
			registrationRecordOutsideMultiLangWriteService.updateLangFields(
					cmd.recordId(), companyId, langPayload, requestLangTag);
		}
		return rowMapAssembler.toApiRow(entity);
	}
}
