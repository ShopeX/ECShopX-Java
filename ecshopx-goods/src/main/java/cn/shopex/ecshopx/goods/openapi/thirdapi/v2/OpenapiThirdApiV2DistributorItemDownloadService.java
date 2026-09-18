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

package cn.shopex.ecshopx.goods.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemDownloadCommand;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.espier.storage.FileStorageService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.DistributorItemQrCodeRedisService;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2DistributorItemDownloadService {

	private static final String TEMPLATE_YYKWEISHOP = "yykweishop";

	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final ItemsRepository itemsRepository;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;
	private final FileStorageService fileStorageService;
	private final DistributorItemQrCodeRedisService distributorItemQrCodeRedisService;

	public OpenapiThirdApiV2DistributorItemDownloadService(
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			ItemsRepository itemsRepository,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient,
			FileStorageService fileStorageService,
			DistributorItemQrCodeRedisService distributorItemQrCodeRedisService) {
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.itemsRepository = itemsRepository;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
		this.fileStorageService = fileStorageService;
		this.distributorItemQrCodeRedisService = distributorItemQrCodeRedisService;
	}

	public Map<String, Object> executeDownload(
			long companyId, OpenapiDistributorItemDownloadCommand command) {
		String wxaAppId =
				weappAuthorizerAppidRepository
						.findAuthorizerAppid(companyId, TEMPLATE_YYKWEISHOP)
						.orElseThrow(
								() -> new ResourceException("没有开通此小程序，不能下载"));

		Map<String, Object> distInfo =
				distributorRepositoryGetInfoSimpleService.getInfoSimpleByShopCode(
						companyId, command.shopCode());
		long distributorId = parseLong(distInfo.get("distributor_id"));
		if (distributorId <= 0L) {
			throw v2Fail(OpenapiErrorCode.DISTRIBUTOR_NOT_FOUND, "未查询到该店铺");
		}

		Items itemInfo = itemsRepository.findByItemBnAndCompany(command.itemCode(), companyId);
		if (itemInfo == null || itemInfo.getItemId() == null || itemInfo.getItemId() <= 0L) {
			throw v2Fail(OpenapiErrorCode.GOODS_NOT_FOUND, "商品找不到");
		}
		long itemId = itemInfo.getItemId();

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("url", resolveQrCodeUrl(companyId, wxaAppId, distributorId, itemId));
		return result;
	}

	private String resolveQrCodeUrl(
			long companyId, String wxaAppId, long distributorId, long itemId) {
		Map<String, Object> tokenResult =
				fileStorageService.getUploadToken("image", companyId, null, null);
		String domain = extractDomain(tokenResult);

		String uri;
		Optional<String> cached =
				distributorItemQrCodeRedisService.hashGet(companyId, distributorId, itemId);
		if (cached.isEmpty()) {
			byte[] qrBytes = fetchDistributionGoodsWxaCodeBytes(wxaAppId, itemId, distributorId);
			uri = uploadDistributorItemQrCodeKey(companyId, qrBytes);
			distributorItemQrCodeRedisService.hashSet(companyId, distributorId, uri);
		} else {
			uri = cached.get();
		}
		return joinDomainAndKey(domain, uri);
	}

	private byte[] fetchDistributionGoodsWxaCodeBytes(
			String wxaAppId, long itemId, long distributorId) {
		String scene = "id=" + itemId + "&dtid=" + distributorId;
		try {
			return wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(
					wxaAppId, scene, "pages/item/espier-detail");
		} catch (Exception e) {
			return wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(
					wxaAppId, scene, "pages/goodsdetail");
		}
	}

	private String uploadDistributorItemQrCodeKey(long companyId, byte[] imageBytes) {
		try {
			String filename = UUID.randomUUID().toString().replace("-", "") + ".jpg";
			Map<String, Object> uploadResult =
					fileStorageService.upload(
							"image", companyId, "distributor_item_qr_code", filename, imageBytes);
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

	private static OpenapiDistributorV2FailException v2Fail(String code, String message) {
		return new OpenapiDistributorV2FailException(code, message);
	}

	private static long parseLong(Object value) {
		if (value == null) {
			return 0L;
		}
		if (value instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(value.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
