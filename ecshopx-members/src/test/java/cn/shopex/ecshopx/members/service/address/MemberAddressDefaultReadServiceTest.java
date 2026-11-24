package cn.shopex.ecshopx.members.service.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.domain.MembersAddress;
import cn.shopex.ecshopx.members.mapper.MembersAddressMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberAddressDefaultReadServiceTest {

	@Mock
	private MembersAddressMapper membersAddressMapper;

	@InjectMocks
	private MemberAddressDefaultReadService service;

	@Test
	void getDefaultAddress_whenDefaultExists_returnsMapAlignedWithAddressListToRow() {
		MembersAddress row = new MembersAddress();
		row.setAddressId(9L);
		row.setCompanyId(38L);
		row.setUserId(261L);
		row.setUsername("张三");
		row.setTelephone("13800138000");
		row.setArea("华东");
		row.setProvince("上海市");
		row.setCity("上海市");
		row.setCounty("浦东新区");
		row.setAdrdetail("世纪大道1号");
		row.setPostalCode("200120");
		row.setIsDef(true);
		row.setCreated(1L);
		row.setUpdated(2L);
		row.setThirdData(null);
		row.setLng("121.5");
		row.setLat("31.2");

		when(membersAddressMapper.selectOne(any(Wrapper.class))).thenReturn(row);

		Map<String, Object> actual = service.getDefaultAddress(38L, 261L);

		assertEquals(MemberAddressResponseMaps.toRow(row), actual);
		assertEquals("张三", actual.get("username"));
		assertEquals("13800138000", actual.get("telephone"));
		assertEquals("上海市", actual.get("province"));
		assertEquals("浦东新区", actual.get("county"));
	}

	@Test
	void getDefaultAddress_whenNoDefault_returnsNull() {
		when(membersAddressMapper.selectOne(any(Wrapper.class))).thenReturn(null);

		assertNull(service.getDefaultAddress(38L, 261L));
	}
}
