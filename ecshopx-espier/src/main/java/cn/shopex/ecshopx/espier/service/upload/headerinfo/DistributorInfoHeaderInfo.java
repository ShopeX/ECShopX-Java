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

package cn.shopex.ecshopx.espier.service.upload.headerinfo;

import cn.shopex.ecshopx.espier.service.upload.UploadHeaderColumnInfo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DistributorInfoHeaderInfo {
	private DistributorInfoHeaderInfo() {}

	public static Map<String, UploadHeaderColumnInfo> map() {
		Map<String, UploadHeaderColumnInfo> map = new LinkedHashMap<>();
		map.put("店铺类型", new UploadHeaderColumnInfo(255, "店铺类型,0:自营", true));
		map.put("店铺号", new UploadHeaderColumnInfo(255, "店铺号，仅可填写英文数字的组合，英文不区分大小写", true));
		map.put("店铺名称", new UploadHeaderColumnInfo(255, "店铺名称", true));
		map.put("门店分类", new UploadHeaderColumnInfo(255, "下拉选取配置的门店分类名称", false));
		map.put("联系人姓名", new UploadHeaderColumnInfo(255, "联系人姓名", true));
		map.put("联系方式", new UploadHeaderColumnInfo(255, "联系方式", true));
		map.put("店铺所在省市区", new UploadHeaderColumnInfo(255, "店铺所在省市区，省市区以英文逗号分隔，仅接受3个层级，位置1省，位置2市，位置3区", true));
		map.put("店铺详细地址", new UploadHeaderColumnInfo(255, "店铺详细地址", true));
		map.put("店铺详细地址门牌号", new UploadHeaderColumnInfo(255, "店铺详细地址门牌号", false));
		map.put("经营开始时间", new UploadHeaderColumnInfo(255, "经营开始时间，以0点开始，半个小时一分隔,excel文本类型", false));
		map.put("经营结束时间", new UploadHeaderColumnInfo(255, "经营结束时间，以0点开始，半个小时一分隔，结束营业时间需小于开始时间,excel文本类型", false));
		map.put("开启快递配送", new UploadHeaderColumnInfo(255, "开启快递配送，是或否", true));
		map.put("自动同步商品", new UploadHeaderColumnInfo(255, "自动同步商品，是或否", true));
		map.put("店铺LOGO", new UploadHeaderColumnInfo(255, "店铺LOGO", false));
		map.put("店铺背景", new UploadHeaderColumnInfo(255, "店铺背景", false));
		map.put("旺店通ERP店铺号", new UploadHeaderColumnInfo(255, "旺店通ERP店铺号", false));
		map.put("聚水潭店铺编号", new UploadHeaderColumnInfo(255, "聚水潭店铺编号", false));
		return Collections.unmodifiableMap(map);
	}
}
