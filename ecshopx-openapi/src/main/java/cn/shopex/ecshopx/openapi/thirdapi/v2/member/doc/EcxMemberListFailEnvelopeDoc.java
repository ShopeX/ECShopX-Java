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

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null：
 * E4002「时间格式有误」（start_date/end_date 键存在且非空白时：纯数字时间戳或非 yyyy-MM-dd HH:mm:ss）；
 * E5002「每页的大小最大为500！」（page_size 原始解析值 ≥500）；
 * E0001「系统异常」（主查询 DataAccessException——含 salesperson_mobile 命中导购时
 * SQL 引用未 JOIN 的 rel_salesperson 别名触发的错误，源码缺陷）。
 * 各过滤键查无多按不过滤或空结果处理，不报错。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxMemberListFailEnvelope")
public class EcxMemberListFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E4002 时间格式有误；E5002 分页格式错误；E0001 系统异常；E4004 未授权", example = "E4002")
	public String code;

	@Schema(description = "错误说明", example = "时间格式有误")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}