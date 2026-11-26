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

package cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；成功信封 data（仅 1 键 url）。
 * {@code OpenapiThirdApiV2DistributorDownloadService#resolveQrCodeUrl}：
 * 存储 domain + 上传 key（group=distributor_qr_code）；Redis 缓存命中直接用缓存 uri
 *（上传失败时缓存空串 → url 仅为 domain）。
 */
@Schema(name = "EcxDistributorDownloadData")
public class EcxDistributorDownloadDataDoc {

	@Schema(
			description =
					"店铺首页小程序码图片 URL（存储 domain + / + 上传 key；key 形如"
							+ " [project/]image/{company_id}/{yyyy/MM/dd}/distributor_qr_code/{md5(时间)}{uuid}.jpg）。"
							+ "上传失败被吞掉时 uri 为空串且写入缓存，url 退化为 domain 本身",
			example = "https://example.test/test-project/image/1/2025/01/01/distributor_qr_code/abc123test.jpg")
	public String url;
}