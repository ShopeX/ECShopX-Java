package cn.shopex.ecshopx.chinaumspay.service.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChinaumsPayDivisionRetResponseCodecTest {

	@Test
	@DisplayName("parseRetId: 与上送 10+0+2+0+3 逆解一致")
	void parseRetId_100203() {
		long[] t = ChinaumsPayDivisionRetResponseCodec.parseRetIdTriple("100203");
		assertThat(t).containsExactly(10L, 2L, 3L);
	}

	@Test
	@DisplayName("parseRetId: 123040506 -> 123,405,6")
	void parseRetId_divisionUploadTimes() {
		long[] t = ChinaumsPayDivisionRetResponseCodec.parseRetIdTriple("123040506");
		assertThat(t).containsExactly(123L, 405L, 6L);
	}

	@Test
	@DisplayName("getBackStatus 未识别银联位 -> 0")
	void umsUnknown_defaults0() {
		assertThat(ChinaumsPayDivisionRetResponseCodec.mapUmsStatusCharToLocalBackStatus("9"))
				.isEqualTo("0");
	}

	@Test
	@DisplayName("整文件错误首行")
	void fileError_firstLine() {
		var r = ChinaumsPayDivisionRetResponseCodec.parseFileContent("VERIFY_FAILED\n");
		assertThat(r.status).isEqualTo(ChinaumsPayDivisionRetResponseCodec.STATUS_FILE_ERROR);
		assertThat(r.fileErrorMessage).isEqualTo("验签失败");
		assertThat(r.dataRows).isEmpty();
	}

	@Test
	@DisplayName("首行列数不符抛错")
	void firstLine_columnMismatch() {
		assertThrows(IllegalStateException.class, () -> ChinaumsPayDivisionRetResponseCodec.parseFileContent("a|b\n"));
	}
}
