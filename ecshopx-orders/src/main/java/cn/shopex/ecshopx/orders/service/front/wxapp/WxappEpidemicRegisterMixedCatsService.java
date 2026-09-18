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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappEpidemicRegisterMixedCatsService {

	private static final BigDecimal BD_36_1 = new BigDecimal("36.1");
	private static final BigDecimal BD_STEP = new BigDecimal("0.1");
	private static final BigDecimal BD_38 = new BigDecimal("38");

	public Map<String, Object> epidemicRegisterMixedCats() {
		List<String> temperature = new ArrayList<>();
		for (BigDecimal i = BD_36_1; i.compareTo(BD_38) < 0; i = i.add(BD_STEP)) {
			temperature.add(i.setScale(1, RoundingMode.DOWN).toPlainString() + "℃");
		}
		temperature.add(0, "36.0℃及以下");
		temperature.add("38.0℃及以上");

		List<String> job = new ArrayList<>(20);
		job.add("非高危职业");
		job.add("进口冷链");
		job.add("口岸检疫");
		job.add("公共交通");
		job.add("生鲜市场");
		job.add("船舶引航");
		job.add("物流运输");
		job.add("航空空勤");
		job.add("家政护理");
		job.add("保安保洁");
		job.add("旅馆酒店");
		job.add("维修装修");
		job.add("个体经营");
		job.add("出国学习工作");
		job.add("教育培训");
		job.add("环卫绿化");
		job.add("养老院");
		job.add("儿童福利院");
		job.add("救助管理机构");
		job.add("集中隔离点和居家隔离服务人员");

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("temperature", temperature);
		result.put("job", job);
		return result;
	}
}
