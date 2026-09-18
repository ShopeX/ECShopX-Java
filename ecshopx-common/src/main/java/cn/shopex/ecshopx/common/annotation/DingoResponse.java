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

package cn.shopex.ecshopx.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 类或方法上，声明该端点的异常响应采用兼容风格（嵌套 {@code data.message} / {@code data.status_code}）。
 * <p>
 * {@code GlobalExceptionHandler} 会自动读取此注解决定错误响应格式，
 * 新增接口无需再手动修改 {@code GlobalExceptionHandler} 的路由表。
 *
 * <pre>
 * // 示例 — 前端 UGC 接口
 * &#64;DingoResponse(
 *     resource  = DingoResponse.ResourceStyle.DINGO,
 *     badRequest = DingoResponse.BadRequestStyle.DINGO_400,
 *     unauthorized = true,

 * )
 * &#64;RestController
 * public class UgcPostController { ... }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DingoResponse {

	/**
	 * {@link ResourceException} 的响应风格，默认 {@link ResourceStyle#DINGO}。
	 */
	ResourceStyle resource() default ResourceStyle.DINGO;

	/**
	 * {@link BadRequestException} 的响应风格，默认 {@link BadRequestStyle#NONE}（使用标准 ApiResult）。
	 */
	BadRequestStyle badRequest() default BadRequestStyle.NONE;

	/**
	 * 是否将 {@code UnauthorizedException} 包装为兼容 401 格式。
	 * 同时会检查"该账号已被禁用."消息并返回 code=401002。
	 */
	boolean unauthorized() default false;

	/**
	 * 与 {@link #unauthorized()} 同时使用时：HTTP 始终为 200，错误体现在 {@code data.status_code}
	 */
	boolean unauthorizedHttpOk() default false;

	/**
	 * 是否将 {@code NoResourceFoundException} 包装为兼容 404 格式。
	 */
	boolean notFound() default false;

	// ─────────────────── ResourceException 处理风格 ───────────────────

	enum ResourceStyle {
		/** dingoStyleError(msg, embedded ?? 422, businessCode) — 最常见 */
		DINGO,
		/** dingoStyleError(msg, 422) — 固定 422，不读 embedded */
		DINGO_FIXED,
		/** 返回标准 422 ApiResult（不使用兼容包装） */
		PLAIN
	}

	// ─────────────────── BadRequestException 处理风格 ───────────────────

	enum BadRequestStyle {
		/** 不做兼容包装，返回标准 ApiResult 400 */
		NONE,
		/** dingoStyleError(msg, embedded ?? 400) */
		DINGO_400,
		/** dingoStyleError(msg, embedded ?? 422) */
		DINGO_422,
		/** dingoStyleError(msg, 422) — 固定 422 */
		DINGO_FIXED
	}
}
