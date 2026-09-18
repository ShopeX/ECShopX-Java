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

package cn.shopex.ecshopx.distribution.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.openapi.OpenapiDistributorOpenApiRowFormatSupport;
import cn.shopex.ecshopx.distribution.service.DistributorListOutsideLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2DistributorDetailService {

	private final DistributorMapper distributorMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DistributorListOutsideLangReadService distributorListOutsideLangReadService;
	private final LangueProperties langueProperties;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2DistributorDetailService(
			DistributorMapper distributorMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DistributorListOutsideLangReadService distributorListOutsideLangReadService,
			LangueProperties langueProperties,
			ObjectMapper objectMapper) {
		this.distributorMapper = distributorMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.distributorListOutsideLangReadService = distributorListOutsideLangReadService;
		this.langueProperties = langueProperties;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiDetail(long companyId, String shopCodeRaw) {
		Distributor entity =
				distributorMapper.selectOne(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, companyId)
								.eq(Distributor::getShopCode, shopCodeRaw)
								.last("LIMIT 1"));

		if (entity == null) {
			throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_NOT_FOUND, "店铺找不到");
		}

		Map<String, Object> row =
				OpenapiDistributorOpenApiRowFormatSupport.toInternalRowMap(
						entity, sensitiveFieldEncryptor, objectMapper);

		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row);
		String requestLang = langueProperties.getDefaultLang();
		distributorListOutsideLangReadService.overlayOpenapiListFields(companyId, requestLang, rows);

		return OpenapiDistributorOpenApiRowFormatSupport.toOpenapiRow(rows.get(0));
	}

	private static OpenapiDistributorV2FailException v2Fail(String code, String message) {
		return new OpenapiDistributorV2FailException(code, message);
	}
}
