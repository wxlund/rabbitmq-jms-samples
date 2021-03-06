package io.pivotal.rabbitmq.jms.boot.demo;

import javax.jms.ConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

@Profile("consume")
@Component
public class SpringRabbitJMSConsumer {

	private static final Log log = LogFactory.getLog(SpringRabbitJMSConsumer.class);

	@Value("${test.queue:test.queue}")
	String queueName;
	
	@Value("${test.delay:100}")
	long delay;

	@Bean
	public DefaultMessageListenerContainer jmsListener(ConnectionFactory connectionFactory) {

		log.info("connectionFactory => " + connectionFactory);

		DefaultMessageListenerContainer jmsListener = new DefaultMessageListenerContainer();
		jmsListener.setConnectionFactory(connectionFactory);
		jmsListener.setDestinationName(queueName);
		jmsListener.setPubSubDomain(false);

		MessageListenerAdapter adapter = new MessageListenerAdapter(new Receiver(this.delay));
		adapter.setDefaultListenerMethod("receive");

		jmsListener.setMessageListener(adapter);
		return jmsListener;
	}

	static class Receiver {
		
		private long delay=100;
		
		public Receiver(long delay) {
			this.delay = delay;
		}
		
		public void receive(String message) {
			if(message.length()>10) { message = message.substring(0, 10)+"..."; }
			System.out.println("Received " + message);
			try {
				Thread.sleep(this.delay);
			} catch(Exception ignore) {}
		}
	}

}
