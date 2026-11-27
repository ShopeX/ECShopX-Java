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

package cn.shopex.ecshopx.adapay.api.admin.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；结算银行列表行，对应运行时
 * {@code AdapayBankCodesListRowDto}（SnakeCase 序列化）。
 */
@Schema(name = "AdapayBankListRow", description = "结算银行列表行（snake_case 序列化）")
public class AdapayBankListRowDoc {

	@Schema(description = "银行记录 id（adapay_bank_codes.id）", example = "8")
	public Long id;

	@Schema(description = "银行名称", example = "中国工商银行")
	public String bank_name;

	@Schema(description = "银行代码（联行号风格 8 位编码字符串，前导零保留）", example = "01020000")
	public String bank_code;
}