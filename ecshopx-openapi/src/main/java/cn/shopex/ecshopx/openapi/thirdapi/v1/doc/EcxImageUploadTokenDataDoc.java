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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code FileStorageService#getUploadToken}：{driver, token}。token 键集随部署存储驱动变化，
 * 下列字段为各驱动键集的并集（字段来源见各 description；同一响应只出现对应驱动的子集）。
 */
@Schema(
		name = "EcxImageUploadTokenData",
		description = "上传凭证：driver + token（键集随驱动：local/oss/aws/qiniu/cosv5）",
		example =
				"{\"driver\":\"local\",\"token\":{\"domain\":\"https://example.test/\","
						+ "\"key\":\"test-project/image/1/2025/01/01/item/abc123test.png\"}}")
public class EcxImageUploadTokenDataDoc {

	@Schema(
			description = "存储驱动名：local / oss / aws / qiniu / cosv5（由部署配置决定）",
			example = "local")
	public String driver;

	@Schema(description = "上传凭证（键集随驱动，见各字段说明）")
	public TokenDoc token;

	@Schema(name = "EcxImageUploadTokenPayload", description = "驱动相关上传凭证字段并集")
	public static class TokenDoc {

		@Schema(
				description = "【local/aws/qiniu/cosv5】访问域名（driver.url(\"\")）",
				example = "https://example.test/")
		public String domain;

		@Schema(
				description =
						"【local/aws/qiniu/cosv5】存储对象键，形如"
								+ " {project}/{filetype}/{companyId}/{yyyy/MM/dd}[/{group}]/{md5(时间)}{filename}",
				example = "test-project/image/1/2025/01/01/item/abc123test.png")
		public String key;

		@Schema(description = "【aws/cosv5】预签名 PUT 上传 URL（1 小时有效）")
		public String url;

		@Schema(description = "【oss】OSS AccessKeyId")
		public String accessid;

		@Schema(description = "【oss/qiniu】直传主机地址", example = "https://upload.qiniup.com")
		public String host;

		@Schema(description = "【oss】Base64 编码的 Post Policy（1 小时有效，key 前缀限定）")
		public String policy;

		@Schema(description = "【oss】Post Policy 签名")
		public String signature;

		@Schema(description = "【oss】凭证过期时间（Unix 秒）")
		public Long expire;

		@JsonProperty("callback-var")
		@ArraySchema(arraySchema = @Schema(description = "【oss】回调变量，恒为空数组"))
		public List<Object> callbackVar;

		@Schema(description = "【oss】上传目录（即对象键）")
		public String dir;

		@Schema(description = "【qiniu】七牛上传凭证 uptoken")
		public String token;

		@Schema(description = "【qiniu】存储区域（如 z0/z1/z2/na0/as0/cn-east-2/cn-north-1）", example = "z0")
		public String region;
	}
}