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

package cn.shopex.ecshopx.promotions.service.export;

import cn.shopex.ecshopx.espier.service.ExportCsvFileService;
import cn.shopex.ecshopx.espier.service.ExportLogCreateService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import cn.shopex.ecshopx.promotions.mapper.TurntableLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TurntableLuckyDrawLogExportAsyncService {

	private static final Logger log = LoggerFactory.getLogger(TurntableLuckyDrawLogExportAsyncService.class);

	private static final String EXPORT_TYPE = "export_luckdraw_log";
	private static final int BATCH = 100;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(SHANGHAI);
	private static final DateTimeFormatter ROW_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(SHANGHAI);

	private final TurntableLogMapper turntableLogMapper;
	private final MemberAccountService memberAccountService;
	private final ExportCsvFileService exportCsvFileService;
	private final ExportLogCreateService exportLogCreateService;

	public TurntableLuckyDrawLogExportAsyncService(
			TurntableLogMapper turntableLogMapper,
			MemberAccountService memberAccountService,
			ExportCsvFileService exportCsvFileService,
			ExportLogCreateService exportLogCreateService) {
		this.turntableLogMapper = turntableLogMapper;
		this.memberAccountService = memberAccountService;
		this.exportCsvFileService = exportCsvFileService;
		this.exportLogCreateService = exportLogCreateService;
	}

	public void runExport(LuckyDrawLogExportJobContext ctx) {
		LambdaQueryWrapper<TurntableLog> countWrapper =
				new LambdaQueryWrapper<TurntableLog>().eq(TurntableLog::getActId, ctx.actId());
		long total = turntableLogMapper.selectCount(countWrapper);
		if (total == 0L) {
			return;
		}
		LinkedHashMap<String, String> titles = new LinkedHashMap<>();
		titles.put("user_id", "用户id");
		titles.put("user_card_code", "会员编码");
		titles.put("mobile", "手机号");
		titles.put("prize_type", "奖品类型");
		titles.put("prize_title", "获取奖品");
		titles.put("created", "中奖时间");
		List<Map<String, String>> allRows = new ArrayList<>();
		LambdaQueryWrapper<TurntableLog> pageWrapper = new LambdaQueryWrapper<TurntableLog>()
				.eq(TurntableLog::getActId, ctx.actId())
				.orderByAsc(TurntableLog::getId);
		int pages = (int) Math.ceil(total / (double) BATCH);
		for (int page = 1; page <= pages; page++) {
			Page<TurntableLog> mpPage = new Page<>(page, BATCH, false);
			turntableLogMapper.selectPage(mpPage, pageWrapper);
			List<TurntableLog> records = mpPage.getRecords();
			List<Long> pageUserIds = records.stream()
					.map(TurntableLog::getUserId)
					.filter(Objects::nonNull)
					.distinct()
					.toList();
			Map<Long, Map<String, Object>> memberByUserId = new HashMap<>();
			if (!pageUserIds.isEmpty()) {
				List<Map<String, Object>> summaries =
						memberAccountService.listMemberSummariesByUserIds(ctx.companyId(), pageUserIds);
				for (Map<String, Object> s : summaries) {
					Object uidObj = s.get("user_id");
					if (uidObj == null) {
						continue;
					}
					long uid = uidObj instanceof Number n ? n.longValue() : Long.parseLong(uidObj.toString().trim());
					memberByUserId.put(uid, s);
				}
			}
			for (TurntableLog logRow : records) {
				Map<String, String> row = new LinkedHashMap<>();
				if (logRow.getUserId() == null) {
					row.put("user_id", "");
				} else {
					row.put("user_id", String.valueOf(logRow.getUserId()));
				}
				Map<String, Object> mem =
						logRow.getUserId() == null ? null : memberByUserId.get(logRow.getUserId());
				String userCardCode = "";
				String mobile = "";
				if (mem != null) {
					Object ucc = mem.get("user_card_code");
					userCardCode = ucc != null ? ucc.toString() : "";
					Object mob = mem.get("mobile");
					mobile = mob != null ? mob.toString() : "";
				}
				row.put("user_card_code", userCardCode);
				row.put("mobile", mobile);
				String pt = logRow.getPrizeType();
				String prizeTypeLabel = "";
				if ("coupon".equals(pt)) {
					prizeTypeLabel = "优惠券";
				} else if ("coupons".equals(pt)) {
					prizeTypeLabel = "券包";
				} else if ("points".equals(pt)) {
					prizeTypeLabel = "积分";
				} else if ("thanks".equals(pt)) {
					prizeTypeLabel = "谢谢惠顾";
				}
				row.put("prize_type", prizeTypeLabel);
				row.put("prize_title", Objects.toString(logRow.getPrizeTitle(), ""));
				long createdSec = logRow.getCreated() == null ? 0L : logRow.getCreated().longValue();
				row.put("created", ROW_TS.format(Instant.ofEpochSecond(createdSec)));
				allRows.add(row);
			}
		}
		String fileBaseName = FILE_TS.format(Instant.now()) + ctx.companyId() + "活动统计导出";
		Map<String, String> uploaded = exportCsvFileService.exportCsv(fileBaseName, titles, allRows);
		if (uploaded.isEmpty() || !StringUtils.hasText(uploaded.get("url"))) {
			log.debug("队列导出: 执行导出时失败");
			return;
		}
		exportLogCreateService.createFinishLog(
				ctx.companyId(),
				ctx.operatorId(),
				ctx.merchantId(),
				ctx.supplierId(),
				EXPORT_TYPE,
				uploaded.getOrDefault("filename", fileBaseName + ".csv"),
				uploaded.get("url"),
				Instant.now().getEpochSecond());
	}
}
