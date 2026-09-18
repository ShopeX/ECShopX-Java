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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.distribution.integration.LocalDeliveryCompanyRelOpenReader;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LocalDeliveryDistributorInfoService {

	private static final Map<String, String> BUSINESS_LIST = new LinkedHashMap<>();

	static {
		BUSINESS_LIST.put("1", "食品小吃");
		BUSINESS_LIST.put("2", "饮料");
		BUSINESS_LIST.put("3", "鲜花绿植");
		BUSINESS_LIST.put("8", "文印票务");
		BUSINESS_LIST.put("9", "便利店");
		BUSINESS_LIST.put("13", "水果生鲜");
		BUSINESS_LIST.put("19", "同城电商");
		BUSINESS_LIST.put("20", "医药");
		BUSINESS_LIST.put("21", "蛋糕");
		BUSINESS_LIST.put("24", "酒品");
		BUSINESS_LIST.put("25", "小商品市场");
		BUSINESS_LIST.put("26", "服装");
		BUSINESS_LIST.put("27", "汽修零配");
		BUSINESS_LIST.put("28", "数码家电");
		BUSINESS_LIST.put("29", "小龙虾");
		BUSINESS_LIST.put("50", "个人");
		BUSINESS_LIST.put("51", "火锅");
		BUSINESS_LIST.put("53", "个护美妆");
		BUSINESS_LIST.put("55", "母婴");
		BUSINESS_LIST.put("57", "家居家纺");
		BUSINESS_LIST.put("59", "手机");
		BUSINESS_LIST.put("61", "家装");
		BUSINESS_LIST.put("5", "其他");
	}

	private final LocalDeliveryCompanyRelOpenReader localDeliveryCompanyRelOpenReader;

	public LocalDeliveryDistributorInfoService(LocalDeliveryCompanyRelOpenReader localDeliveryCompanyRelOpenReader) {
		this.localDeliveryCompanyRelOpenReader = localDeliveryCompanyRelOpenReader;
	}

	public Map<String, String> businessList() {
		return BUSINESS_LIST;
	}

	public boolean isLocalDeliveryOpen(long companyId) {
		return localDeliveryCompanyRelOpenReader.readIsOpen(companyId);
	}
}
