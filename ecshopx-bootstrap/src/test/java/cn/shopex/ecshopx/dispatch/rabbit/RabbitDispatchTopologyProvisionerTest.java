package cn.shopex.ecshopx.dispatch.rabbit;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

class RabbitDispatchTopologyProvisionerTest {

    @Test
    void provisionDeclaresDirectExchangeQueueAndBindingForDefaultQueue() {
        RabbitAdmin rabbitAdmin = Mockito.mock(RabbitAdmin.class);
        RabbitDispatchTopologyProvisioner provisioner = new RabbitDispatchTopologyProvisioner(rabbitAdmin, "dispatch.direct", "dispatch.queue.");

        provisioner.provisionQueue("default");

        ArgumentCaptor<DirectExchange> exchangeCaptor = ArgumentCaptor.forClass(DirectExchange.class);
        ArgumentCaptor<Queue> queueCaptor = ArgumentCaptor.forClass(Queue.class);
        ArgumentCaptor<Binding> bindingCaptor = ArgumentCaptor.forClass(Binding.class);
        verify(rabbitAdmin).declareExchange(exchangeCaptor.capture());
        verify(rabbitAdmin).declareQueue(queueCaptor.capture());
        verify(rabbitAdmin).declareBinding(bindingCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("dispatch.direct", exchangeCaptor.getValue().getName());
        org.junit.jupiter.api.Assertions.assertEquals("dispatch.queue.default", queueCaptor.getValue().getName());
        org.junit.jupiter.api.Assertions.assertEquals("default", bindingCaptor.getValue().getRoutingKey());
    }

    @Test
    void provisionDeclaresDedicatedQueueForNamedRoutingKey() {
        RabbitAdmin rabbitAdmin = Mockito.mock(RabbitAdmin.class);
        RabbitDispatchTopologyProvisioner provisioner = new RabbitDispatchTopologyProvisioner(rabbitAdmin, "dispatch.direct", "dispatch.queue.");

        provisioner.provisionQueue("sms");

        ArgumentCaptor<Queue> queueCaptor = ArgumentCaptor.forClass(Queue.class);
        ArgumentCaptor<Binding> bindingCaptor = ArgumentCaptor.forClass(Binding.class);
        verify(rabbitAdmin).declareQueue(queueCaptor.capture());
        verify(rabbitAdmin).declareBinding(bindingCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("dispatch.queue.sms", queueCaptor.getValue().getName());
        org.junit.jupiter.api.Assertions.assertEquals("sms", bindingCaptor.getValue().getRoutingKey());
    }
}
