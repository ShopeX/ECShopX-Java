package cn.shopex.ecshopx.aftersales.service;

import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;

final class AftersalesTestMessageSource {

	static final MessageSource FALLBACK = new StaticMessageSource();

	private AftersalesTestMessageSource() {}
}
