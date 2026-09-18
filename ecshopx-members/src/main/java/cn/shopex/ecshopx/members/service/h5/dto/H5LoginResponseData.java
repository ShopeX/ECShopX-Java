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

package cn.shopex.ecshopx.members.service.h5.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 登录接口 data：成功时 token 为 JWT 字符串；软失败时为 {@link Boolean#FALSE}。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class H5LoginResponseData {

	private Object token;

	public H5LoginResponseData() {
	}

	public H5LoginResponseData(String jwtToken) {
		this.token = jwtToken;
	}

	public H5LoginResponseData(Boolean tokenFalse) {
		this.token = tokenFalse;
	}

	public Object getToken() {
		return token;
	}

	public void setToken(Object token) {
		this.token = token;
	}
}
