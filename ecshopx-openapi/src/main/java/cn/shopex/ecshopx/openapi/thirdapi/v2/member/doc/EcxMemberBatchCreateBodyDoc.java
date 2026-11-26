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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，data 键双通道
 * （{@code OpenapiMemberV2BatchCreateParams}：body 含 data 键或 query 参数表含 data 即 hasData；
 * 值走 originalString，body 数组会被字符串化后 JSON 解析）。
 * 同步校验见 {@code OpenapiThirdApiV2MemberCreateService#validateDatumAndToPhpFormData}
 * （复用单个创建的 validateAndNormalize），逐条入队见
 * {@code OpenapiThirdApiV2MemberBatchCreateService}。
 */
@Schema(
		name = "EcxMemberBatchCreateBody",
		description =
				"批量创建会员（异步）参数：仅 data 一键。data 键缺席 → 直接返回 status=0（不报错不入队）；"
						+ "键存在但值空白/JSON 解析失败/非数组 → 空列表 → status=1（0 条入队，解析失败静默）；"
						+ "元素非对象被静默丢弃；>50 条报 E5500「批量创建最多支持50个！」。"
						+ "逐条同步校验通过后全部入队（dispatch 异步创建），返回 status=1；"
						+ "手机号占用/推荐人查无/等级卡号校验等发生在异步 job 内，不影响同步响应。")
public class EcxMemberBatchCreateBodyDoc {

	@Schema(
			description =
					"会员数组（JSON 数组或 JSON 数组字符串；body 可直接传数组）。≤50 条；"
							+ "每元素字段与单个创建一致（见 EcxMemberBatchCreateItemDoc）；"
							+ "键缺席返回 status=0，解析失败静默按空数组",
			example = "[{\"mobile\":\"13800138000\"}]")
	public List<EcxMemberBatchCreateItemDoc> data;
}