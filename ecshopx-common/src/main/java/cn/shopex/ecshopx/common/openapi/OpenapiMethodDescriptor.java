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

package cn.shopex.ecshopx.common.openapi;

/**
 * {@code openapi_list.csv} 单行描述符。
 *
 * @param method 点分方法名，如 {@code ecx.order.writeoff}
 * @param httpVerb GET/POST/…
 * @param version 规范版本 {@code 1.0} / {@code 2.0}
 * @param javaVersion 短版本 {@code v1} / {@code v2}
 * @param javaClass CSV java-class 列
 * @param javaMethod CSV java-method 列
 */
public record OpenapiMethodDescriptor(
		String method,
		String httpVerb,
		String version,
		String javaVersion,
		String javaClass,
		String javaMethod) {

	public String routeKey() {
		return version + " " + httpVerb + " " + method;
	}
}
