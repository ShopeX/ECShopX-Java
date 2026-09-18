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

package cn.shopex.ecshopx.common.distribution;

/** 按企业地图配置对地址做地理编码（高德 / 腾讯）。 */
public interface CompanyMapGeocodePort {

	GeocodeLatLng geocode(long companyId, String city, String address);

	/**
	 * 与 {@link #geocode} 相同数据源与解析规则，但无法解析出坐标时返回 {@code null}（不抛出「地址识别失败」类业务异常）。
	 * 企业地图配置缺失或类型无效时仍抛 {@link cn.shopex.ecshopx.common.exception.ResourceException}。
	 */
	GeocodeLatLng geocodeAllowEmpty(long companyId, String city, String address);

	/**
	 * 使用企业默认地图配置做正向地址解析的第三方原始结果：腾讯为整段 JSON 对象；高德为 {@code geocodes} 数组。
	 */
	Object getLatAndLngByPositionRaw(long companyId, String address);

	/**
	 * 使用企业默认地图配置做逆地理编码。腾讯返回 {@code result} 对象；高德映射为含 {@code address}
	 * 与 {@code address_component} 的同等形态。失败（含非数字经纬度）返回空列表。
	 */
	Object getPositionByLatAndLngRaw(long companyId, String lat, String lng);

	record GeocodeLatLng(String lat, String lng) {}
}
