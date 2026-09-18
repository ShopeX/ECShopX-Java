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

package cn.shopex.ecshopx.common.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将请求体解析为方法参数，同时支持：
 * <ul>
 *   <li>{@code Content-Type: application/json}</li>
 *   <li>{@code multipart/form-data}</li>
 *   <li>{@code application/x-www-form-urlencoded}</li>
 * </ul>
 * 与 {@code @RequestBody} 二选一；需要校验时配合 {@code @Valid} / {@code @Validated}。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FlexibleBody {

	/**
	 * 为 {@code false} 时，空请求体或空 JSON 对象可解析为 {@code null}（或 Map 的空实例，见具体类型）。
	 */
	boolean required() default true;
}
