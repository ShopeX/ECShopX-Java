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

package cn.shopex.ecshopx.distribution.service.espier;

import cn.shopex.ecshopx.common.espier.upload.EspierImportRowSink;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.DistributorWhiteList;
import cn.shopex.ecshopx.distribution.mapper.DistributorWhiteListMapper;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.distribution.service.DistributorWhiteListAddService;
import cn.shopex.ecshopx.distribution.service.dto.DistributorWhiteListAddCommand;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UploadDistributorWhiteEspierImportRowSink implements EspierImportRowSink {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final DistributorWhiteListAddService distributorWhiteListAddService;
	private final DistributorWhiteListMapper distributorWhiteListMapper;

	public UploadDistributorWhiteEspierImportRowSink(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			DistributorWhiteListAddService distributorWhiteListAddService,
			DistributorWhiteListMapper distributorWhiteListMapper) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.distributorWhiteListAddService = distributorWhiteListAddService;
		this.distributorWhiteListMapper = distributorWhiteListMapper;
	}

	@Override
	public String supportedFileType() {
		return "upload_distributor_white";
	}

	@Override
	public void acceptRow(
			long companyId,
			long operatorId,
			long distributorId,
			long supplierId,
			long merchantId,
			Map<String, Object> row,
			@SuppressWarnings("unused") String operatorType) {
		String mobile = trim(row.get("mobile"));
		String username = trim(row.get("username"));
		String distributorNo = trim(row.get("distributor_no"));
		if (!StringUtils.hasText(mobile)) {
			throw new BadRequestException("手机号不能为空");
		}
		if (!StringUtils.hasText(username)) {
			throw new BadRequestException("姓名不能为空");
		}
		if (!StringUtils.hasText(distributorNo)) {
			throw new BadRequestException("店铺号不能为空");
		}
		Map<String, Object> dist = distributorRepositoryGetInfoSimpleService.getInfoSimpleByShopCode(companyId, distributorNo);
		if (dist == null || dist.isEmpty()) {
			throw new ResourceException("店铺不存在");
		}
		Object didObj = dist.get("distributor_id");
		if (didObj == null) {
			throw new ResourceException("店铺不存在");
		}
		long distId;
		if (didObj instanceof Number n) {
			distId = n.longValue();
		} else {
			try {
				distId = Long.parseLong(String.valueOf(didObj).trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("店铺不存在");
			}
		}
		if (distId <= 0L) {
			throw new ResourceException("店铺不存在");
		}
		long existing =
				distributorWhiteListMapper.selectCount(
						Wrappers.<DistributorWhiteList>lambdaQuery()
								.eq(DistributorWhiteList::getCompanyId, companyId)
								.eq(DistributorWhiteList::getDistributorId, distId)
								.eq(DistributorWhiteList::getMobile, mobile.trim()));
		if (existing > 0L) {
			return;
		}
		DistributorWhiteListAddCommand cmd = new DistributorWhiteListAddCommand(null, List.of(distId), mobile, username);
		distributorWhiteListAddService.addWhiteList(companyId, cmd, "zh-CN");
	}

	private static String trim(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
