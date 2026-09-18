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

package cn.shopex.ecshopx.hfpay.mapper;

import cn.shopex.ecshopx.hfpay.domain.HfpayTradeRecord;
import cn.shopex.ecshopx.hfpay.dto.HfpayProfitBrokerageRow;
import cn.shopex.ecshopx.hfpay.dto.HfpayProfitOrderContextRow;
import cn.shopex.ecshopx.hfpay.dto.HfpayRefundLedgerContextRow;
import cn.shopex.ecshopx.hfpay.dto.TradePayGateRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HfpayTradeRecordMapper extends BaseMapper<HfpayTradeRecord> {

	/**
	 * 提现类支出汇总：fin_type=500、trade_type=0、trade_time 不晚于日末 unix；outcome 按库内行求和，单位分。
	 */
	Long sumOutcomeFenByWithdrawalFilter(
			@Param("companyId") long companyId,
			@Param("distributorId") String distributorId,
			@Param("endUnix") long endUnix);

	TradePayGateRow selectLatestSuccessTradeForHfpayPaySuccess(@Param("orderId") String orderId);

	long countPaySuccessFinRecordsByOuterOrderId(@Param("orderId") String orderId);

	HfpayRefundLedgerContextRow selectRefundLedgerContext(
			@Param("orderId") String orderId, @Param("refundBn") long refundBn);

	long countRefundSeriesLedgerByOuterOrderAndRefund(
			@Param("outerOrderId") String outerOrderId, @Param("refundBn") long refundBn);

	HfpayProfitOrderContextRow selectProfitOrderContext(@Param("orderId") String orderId);

	List<HfpayProfitBrokerageRow> selectProfitBrokerageRows(
			@Param("companyId") long companyId, @Param("orderId") String orderId);
}
