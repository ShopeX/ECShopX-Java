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

package cn.shopex.ecshopx.goods.service.export;

import cn.shopex.ecshopx.goods.service.EpidemicRegisterCsvExportService;
import cn.shopex.ecshopx.orders.repository.OrderEpidemicRegisterListFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EpidemicRegisterExportFileJobHandler {

	private static final Logger log = LoggerFactory.getLogger(EpidemicRegisterExportFileJobHandler.class);

	private final EpidemicRegisterCsvExportService epidemicRegisterCsvExportService;

	public EpidemicRegisterExportFileJobHandler(EpidemicRegisterCsvExportService epidemicRegisterCsvExportService) {
		this.epidemicRegisterCsvExportService = epidemicRegisterCsvExportService;
	}

	public void run(OrderEpidemicRegisterListFilter filter, long operatorId, boolean datapassBlock) {
		try {
			epidemicRegisterCsvExportService.runExport(filter, operatorId, datapassBlock);
		} catch (Exception e) {
			log.error("epidemic register export job failed", e);
		}
	}
}
