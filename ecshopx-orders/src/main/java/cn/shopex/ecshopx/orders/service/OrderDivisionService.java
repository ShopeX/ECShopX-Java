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

package cn.shopex.ecshopx.orders.service;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.orders.mapper.OrderDivisionTransferScheduleMapper;
import cn.shopex.ecshopx.orders.service.division.DivisionFormatResult;
import cn.shopex.ecshopx.orders.service.division.OrderDivisionRelStatus;
import cn.shopex.ecshopx.chinaumspay.service.transfer.ChinaumsPayDivisionDoTransferDownloadSftpService;
import cn.shopex.ecshopx.chinaumspay.service.transfer.ChinaumsPayDivisionDoTransferResubmitSftpService;
import cn.shopex.ecshopx.chinaumspay.service.transfer.ChinaumsPayDivisionDoTransferSftpService;
import cn.shopex.ecshopx.chinaumspay.service.transfer.ChinaumsPayDivisionFormatTransferDataService;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionUploadLog;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionUploadLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * 划付 / 分账上传 SFTP 编排，与 {@code OrderDivisionService::scheduleTransferSftp} 主流程一致。
 */
@Slf4j
@Service
public class OrderDivisionService {

	private final OrderDivisionTransferScheduleMapper orderDivisionTransferScheduleMapper;
	private final CompanysMapper companysMapper;
	private final ChinaumsPayDivisionFormatTransferDataService chinaumsPayDivisionFormatTransferDataService;
	private final ChinaumsPayDivisionDoTransferSftpService chinaumsPayDivisionDoTransferSftpService;
	private final ChinaumsPayDivisionDoTransferResubmitSftpService chinaumsPayDivisionDoTransferResubmitSftpService;
	private final ChinaumspayDivisionUploadLogMapper chinaumspayDivisionUploadLogMapper;
	private final ChinaumsPayDivisionDoTransferDownloadSftpService chinaumsPayDivisionDoTransferDownloadSftpService;
	private final Clock clock;

	public OrderDivisionService(
			OrderDivisionTransferScheduleMapper orderDivisionTransferScheduleMapper,
			CompanysMapper companysMapper,
			ChinaumsPayDivisionFormatTransferDataService chinaumsPayDivisionFormatTransferDataService,
			ChinaumsPayDivisionDoTransferSftpService chinaumsPayDivisionDoTransferSftpService,
			ChinaumsPayDivisionDoTransferResubmitSftpService chinaumsPayDivisionDoTransferResubmitSftpService,
			ChinaumspayDivisionUploadLogMapper chinaumspayDivisionUploadLogMapper,
			ChinaumsPayDivisionDoTransferDownloadSftpService chinaumsPayDivisionDoTransferDownloadSftpService,
			@Nullable Clock clock) {
		this.orderDivisionTransferScheduleMapper = orderDivisionTransferScheduleMapper;
		this.companysMapper = companysMapper;
		this.chinaumsPayDivisionFormatTransferDataService = chinaumsPayDivisionFormatTransferDataService;
		this.chinaumsPayDivisionDoTransferSftpService = chinaumsPayDivisionDoTransferSftpService;
		this.chinaumsPayDivisionDoTransferResubmitSftpService = chinaumsPayDivisionDoTransferResubmitSftpService;
		this.chinaumspayDivisionUploadLogMapper = chinaumspayDivisionUploadLogMapper;
		this.chinaumsPayDivisionDoTransferDownloadSftpService = chinaumsPayDivisionDoTransferDownloadSftpService;
		this.clock = clock == null ? Clock.systemDefaultZone() : clock;
	}

	/** 最外层与 PHP 一致在成功时返回 true；无数据为早退的 true。 */
	public boolean scheduleTransferSftp() {
		log.info("划付上传开始");
		int now = (int) clock.instant().getEpochSecond();
		long need = orderDivisionTransferScheduleMapper.countNeedTransfer(now);
		if (need == 0) {
			log.info("没有需要划付的数据");
			return true;
		}
		QueryWrapper<Companys> w = new QueryWrapper<>();
		w.select("company_id");
		List<Companys> companies = companysMapper.selectList(w);
		for (Companys co : companies) {
			if (co == null || co.getCompanyId() == null) {
				continue;
			}
			long companyId = co.getCompanyId();
			List<Long> distIds = orderDivisionTransferScheduleMapper.listDistributorIds(companyId, now);
			List<DivisionFormatResult> divisionListData = new ArrayList<>();
			for (Long distId : distIds) {
				if (distId == null) {
					continue;
				}
				var rows = orderDivisionTransferScheduleMapper.listNeedTransferByDistributor(distId, now);
				var fr = chinaumsPayDivisionFormatTransferDataService.formatTransferData(companyId, distId, rows);
				if (fr == null) {
					continue;
				}
				divisionListData.add(fr);
			}
			if (divisionListData.isEmpty()) {
				log.info("company_id:{},没有需要划付的数据", companyId);
				continue;
			}
			chinaumsPayDivisionDoTransferSftpService.doTransferSftp(companyId, divisionListData, OrderDivisionRelStatus.UPLOADED);
		}
		log.info("划付上传结束");
		return true;
	}

	/**
	 * 重新提交划付（消费 {@code is_resubmit=WAITING} 错误日志+历史 {@code upload_detail}），不更新
	 * {@code orders_rel_chinaumspay_division}。
	 */
	public boolean scheduleTransferResubmitSftp() {
		log.info("重新提交划付上传开始");
		long resubmitCount = chinaumsPayDivisionDoTransferResubmitSftpService.getErrorlogResubmitCount();
		if (resubmitCount == 0) {
			log.info("没有需要重试的划付数据");
			return true;
		}
		QueryWrapper<Companys> w = new QueryWrapper<>();
		w.select("company_id");
		List<Companys> companies = companysMapper.selectList(w);
		for (Companys co : companies) {
			if (co == null || co.getCompanyId() == null) {
				continue;
			}
			chinaumsPayDivisionDoTransferResubmitSftpService.doTransferResubmitSftp(co.getCompanyId());
		}
		log.info("重新提交划付上传结束");
		return true;
	}

	/**
	 * 按上传日志行下载日终 .ret 并回写库；最外层在成功时返回 true；无数据时早退为 true。
	 */
	public boolean scheduleTransferDownloadSftp() {
		log.info("回盘开始");
		var w = new LambdaQueryWrapper<ChinaumspayDivisionUploadLog>();
		w.eq(ChinaumspayDivisionUploadLog::getBackStatus, "0");
		var list = chinaumspayDivisionUploadLogMapper.selectList(w);
		if (list == null || list.isEmpty()) {
			log.info("没有需要回盘的文件");
			return true;
		}
		for (ChinaumspayDivisionUploadLog row : list) {
			if (row == null) {
				continue;
			}
			chinaumsPayDivisionDoTransferDownloadSftpService.doTransferDownloadSftp(row);
		}
		log.info("回盘结束");
		return true;
	}
}
