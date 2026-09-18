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

package cn.shopex.ecshopx.adapay.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdapayTradeListForInfoQueryService {

	private static final String SQL = """
			SELECT
			  a.trade_id AS tradeId,
			  a.pay_type AS payType,
			  a.order_id AS orderId,
			  a.total_fee AS totalFee,
			  a.pay_fee AS payFee,
			  COALESCE(SUM(b.refunded_fee), 0) AS refundedFee,
			  a.adapay_div_status AS adapayDivStatus,
			  a.adapay_fee_mode AS adapayFeeMode,
			  a.adapay_fee AS adapayFee,
			  a.time_start AS timeStart,
			  a.distributor_id AS distributorId,
			  a.pay_channel AS payChannel,
			  c.order_auto_close_aftersales_time AS closeAftersalesTime,
			  c.order_id AS normalOrderId
			FROM trade a
			LEFT JOIN aftersales_refund b
			  ON a.order_id = b.order_id AND b.refund_status = 'SUCCESS'
			LEFT JOIN orders_normal_orders c ON a.order_id = c.order_id
			WHERE a.company_id = :companyId
			  AND a.order_id = :orderId
			  AND a.trade_state IN ('SUCCESS')
			  AND a.trade_source_type <> 'membercard'
			GROUP BY
			  a.trade_id, a.pay_type, a.order_id, a.total_fee, a.pay_fee,
			  a.adapay_div_status, a.adapay_fee_mode, a.adapay_fee, a.time_start,
			  a.distributor_id, a.pay_channel,
			  c.order_auto_close_aftersales_time, c.order_id
			ORDER BY a.time_start DESC
			""";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public AdapayTradeListForInfoQueryService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public List<Map<String, Object>> listAggregatedForOrder(long companyId, String orderId) {
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("companyId", String.valueOf(companyId))
				.addValue("orderId", orderId);
		return namedParameterJdbcTemplate.query(SQL, params, AGG_ROW_MAPPER);
	}

	private static final RowMapper<Map<String, Object>> AGG_ROW_MAPPER = (rs, rowNum) -> mapAggregatedRow(rs);

	private static Map<String, Object> mapAggregatedRow(ResultSet rs) throws SQLException {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("tradeId", rs.getString("tradeId"));
		row.put("payType", rs.getString("payType"));
		row.put("orderId", rs.getString("orderId"));
		row.put("totalFee", readIntOrZero(rs, "totalFee"));
		int payFee = readIntOrZero(rs, "payFee");
		row.put("payFee", payFee);
		long refundedFee = rs.getLong("refundedFee");
		row.put("refundedFee", refundedFee);
		row.put("adapayDivStatus", rs.getString("adapayDivStatus"));
		row.put("adapayFeeMode", rs.getString("adapayFeeMode"));
		row.put("adapayFee", readIntOrZero(rs, "adapayFee"));
		row.put("timeStart", rs.getString("timeStart"));
		row.put("distributorId", rs.getString("distributorId"));
		row.put("payChannel", rs.getString("payChannel"));
		row.put("closeAftersalesTime", rs.getObject("closeAftersalesTime"));
		row.put("normalOrderId", rs.getObject("normalOrderId"));
		String tradeState;
		if (refundedFee <= 0L) {
			tradeState = "SUCCESS";
		} else if (refundedFee < payFee) {
			tradeState = "PARTIAL_REFUND";
		} else {
			tradeState = "FULL_REFUND";
		}
		row.put("tradeState", tradeState);
		return row;
	}

	private static int readIntOrZero(ResultSet rs, String col) throws SQLException {
		int v = rs.getInt(col);
		return rs.wasNull() ? 0 : v;
	}
}
