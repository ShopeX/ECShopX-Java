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

package cn.shopex.ecshopx.wechat.wxjava;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import me.chanjar.weixin.common.bean.WxAccessToken;
import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;

public final class WxJavaExceptions {

	private WxJavaExceptions() {}

	public static ResourceException toResource(WxErrorException e) {
		WxError err = e.getError();
		String msg = err != null && err.getErrorMsg() != null ? err.getErrorMsg() : e.getMessage();
		return new ResourceException(msg != null ? msg : "微信接口调用失败");
	}

	public static RuntimeException toBadRequestOrUnauthorized(WxErrorException e) {
		WxError err = e.getError();
		int ec = err != null ? err.getErrorCode() : -1;
		String msg = err != null && err.getErrorMsg() != null ? err.getErrorMsg() : "微信接口错误";
		if (ec == 40001 || ec == 40014 || ec == 41001 || ec == 42001) {
			return new UnauthorizedException(msg);
		}
		if (ec == -1) {
			return new BadRequestException("微信系统繁忙, 请稍后重试", 500);
		}
		return new BadRequestException(msg, ec);
	}

	public static WxAccessToken wxAccessToken(String token, int expiresInSeconds) {
		WxAccessToken t = new WxAccessToken();
		t.setAccessToken(token);
		t.setExpiresIn(expiresInSeconds);
		return t;
	}
}
