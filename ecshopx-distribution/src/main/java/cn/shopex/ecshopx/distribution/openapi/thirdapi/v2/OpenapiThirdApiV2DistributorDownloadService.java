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

import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.service.DistributorQrCodeRedisService;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2DistributorDownloadService {

	private final DistributorMapper distributorMapper;
	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;
	private final FileStorageService fileStorageService;
	private final DistributorQrCodeRedisService distributorQrCodeRedisService;

	public OpenapiThirdApiV2DistributorDownloadService(
			DistributorMapper distributorMapper,
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient,
			FileStorageService fileStorageService,
			DistributorQrCodeRedisService distributorQrCodeRedisService) {
		this.distributorMapper = distributorMapper;
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
		this.fileStorageService = fileStorageService;
		this.distributorQrCodeRedisService = distributorQrCodeRedisService;
	}

	public Map<String, Object> executeOpenapiDownload(
			long companyId,
			String shopCodeRaw,
			String distributorNameRaw,
			String statusRaw,
			String templateNameRaw) {
		LambdaQueryWrapper<Distributor> wrapper =
				new LambdaQueryWrapper<Distributor>().eq(Distributor::getCompanyId, companyId);
		applyDownloadFilters(wrapper, shopCodeRaw, distributorNameRaw, statusRaw);
		wrapper.last("LIMIT 1");
		Distributor entity = distributorMapper.selectOne(wrapper);
		if (entity == null) {
			throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_NOT_FOUND, "店铺找不到");
		}
		long distributorId = entity.getDistributorId();

		String templateName =
				(templateNameRaw == null || templateNameRaw.isBlank())
						? ""
						: templateNameRaw.trim();
		String wxaAppId =
				weappAuthorizerAppidRepository
						.findAuthorizerAppid(companyId, templateName)
						.orElseThrow(
								() ->
										v2Fail(
												OpenapiErrorCode.WECHAT_ERROR,
												"没有开通此小程序"));

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("url", resolveQrCodeUrl(companyId, wxaAppId, distributorId));
		return result;
	}

	private String resolveQrCodeUrl(long companyId, String wxaAppId, long distributorId) {
		Map<String, Object> tokenResult =
				fileStorageService.getUploadToken("image", companyId, null, null);
		String domain = extractDomain(tokenResult);

		String uri;
		Optional<String> cached = distributorQrCodeRedisService.hashGet(companyId, distributorId);
		if (cached.isEmpty()) {
			byte[] qrBytes =
					wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(
							wxaAppId, "dtid=" + distributorId, "pages/index");
			uri = uploadDistributorQrCodeKey(companyId, qrBytes);
			distributorQrCodeRedisService.hashSet(companyId, distributorId, uri);
		} else {
			uri = cached.get();
		}
		return joinDomainAndKey(domain, uri);
	}

	private String uploadDistributorQrCodeKey(long companyId, byte[] imageBytes) {
		try {
			String filename = UUID.randomUUID().toString().replace("-", "") + ".jpg";
			Map<String, Object> uploadResult =
					fileStorageService.upload(
							"image", companyId, "distributor_qr_code", filename, imageBytes);
			Object tokenObj = uploadResult.get("token");
			if (!(tokenObj instanceof Map<?, ?> token)) {
				return "";
			}
			Object keyObj = token.get("key");
			return keyObj == null ? "" : keyObj.toString();
		} catch (Exception ignored) {
			return "";
		}
	}

	private static String extractDomain(Map<String, Object> tokenResult) {
		if (tokenResult == null) {
			return "";
		}
		Object tokenObj = tokenResult.get("token");
		if (!(tokenObj instanceof Map<?, ?> token)) {
			return "";
		}
		Object domainObj = token.get("domain");
		return domainObj == null ? "" : domainObj.toString();
	}

	private static String joinDomainAndKey(String domain, String uri) {
		String d = domain == null ? "" : domain.replaceAll("/+$", "");
		String u = uri == null ? "" : uri.replaceAll("^/+", "");
		if (u.isEmpty()) {
			return d;
		}
		return d + "/" + u;
	}

	private static void applyDownloadFilters(
			LambdaQueryWrapper<Distributor> wrapper,
			String shopCodeRaw,
			String distributorNameRaw,
			String statusRaw) {
		if (isPresentNonEmpty(shopCodeRaw)) {
			wrapper.eq(Distributor::getShopCode, shopCodeRaw);
		}
		if (isPresentNonEmpty(distributorNameRaw)) {
			wrapper.like(Distributor::getName, distributorNameRaw);
		}
		applyStatusFilter(wrapper, statusRaw);
	}

	private static void applyStatusFilter(LambdaQueryWrapper<Distributor> wrapper, String statusRaw) {
		if (!isPresentNonEmpty(statusRaw)) {
			return;
		}
		String isValid = mapStatusToIsValidLoose(statusRaw);
		if (isValid != null) {
			wrapper.eq(Distributor::getIsValid, isValid);
		}
	}

	private static String mapStatusToIsValidLoose(String statusRaw) {
		String trimmed = statusRaw.trim();
		try {
			int status;
			if (trimmed.contains(".")) {
				status = (int) Double.parseDouble(trimmed);
			} else {
				status = Integer.parseInt(trimmed);
			}
			switch (status) {
				case 0:
					return "delete";
				case 1:
					return "true";
				case 2:
					return "false";
				default:
					return null;
			}
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean isPresentNonEmpty(String raw) {
		return raw != null && !raw.isEmpty();
	}

	private static OpenapiDistributorV2FailException v2Fail(String code, String message) {
		return new OpenapiDistributorV2FailException(code, message);
	}
}
