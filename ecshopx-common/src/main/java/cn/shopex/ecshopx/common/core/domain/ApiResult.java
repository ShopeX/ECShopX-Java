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

package cn.shopex.ecshopx.common.core.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 统一 API 响应体。
 * <p>
 * 成功：{@code ApiResult.ok(data)} → {@code {"code":200,"msg":"success","data":...}}
 * <br>
 * 失败：{@code ApiResult.fail(422,"xxx")} → {@code {"code":422,"msg":"xxx"}}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> implements Serializable {

	private int code;
	private String msg;
	private T data;

	private ApiResult() {}

	private ApiResult(int code, String msg, T data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

	public static <T> ApiResult<T> ok() {
		return new ApiResult<>(200, "success", null);
	}

	public static <T> ApiResult<T> ok(T data) {
		return new ApiResult<>(200, "success", data);
	}

	public static <T> ApiResult<T> ok(String msg, T data) {
		return new ApiResult<>(200, msg, data);
	}

	public static <T> ApiResult<T> fail(int code, String msg) {
		return new ApiResult<>(code, msg, null);
	}

	public static <T> ApiResult<T> fail(String msg) {
		return new ApiResult<>(500, msg, null);
	}

	public int getCode() { return code; }
	public void setCode(int code) { this.code = code; }
	public String getMsg() { return msg; }
	public void setMsg(String msg) { this.msg = msg; }
	public T getData() { return data; }
	public void setData(T data) { this.data = data; }
}
