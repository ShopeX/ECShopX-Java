package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.promotions.mapper.WxaNoticeTemplateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxaTemplateMsgListServiceMemberSigninTest {

	@Mock private WxaNoticeTemplateMapper wxaNoticeTemplateMapper;

	@Test
	void listTemplateIds_unknownSourceType_returnsEmpty() {
		WxaTemplateMsgListService service = new WxaTemplateMsgListService(wxaNoticeTemplateMapper);
		assertThat(service.listTemplateIds(1L, "unknown", "yykweishop")).isEmpty();
	}
}
