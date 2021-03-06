package io.pivotal.pa.rabbitmq.jms.raw.config;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.rabbitmq.jms.admin.RMQDestination;

import io.pivotal.pa.rabbitmq.jms.raw.client.JMSClientWorker;
import io.pivotal.pa.rabbitmq.jms.raw.client.MessageConsumerClient;
import io.pivotal.pa.rabbitmq.jms.raw.client.SimpleMessageListener;
import io.pivotal.pa.rabbitmq.jms.raw.properties.AMQPProperties;
import io.pivotal.pa.rabbitmq.jms.raw.properties.AppProperties;
import io.pivotal.pa.rabbitmq.jms.raw.properties.JMSProperties;

@Profile({"consume", "subscribe"})
@Configuration
public class ConsumerConfig {
	
	private static Logger log = LoggerFactory.getLogger(ConsumerConfig.class);

	@Bean
	public JMSClientWorker messageConsumerClient() {
		return new MessageConsumerClient();
	}

	@Bean
	public SimpleMessageListener simpleMessageListener() {
		return new SimpleMessageListener();
	}
		
	@Profile("consume")
	@Bean
	public MessageConsumer messageConsumer(Session jmsSession, 
			                               SimpleMessageListener simpleMessageListener, 
			                               AMQPProperties amqpProperties,
			                               JMSProperties jmsProperties) {
		MessageConsumer messageConsumer = null;
		try {
			if(amqpProperties.amqpQueueName != null && !"".equals(amqpProperties.amqpQueueName)) {
				log.info("rmqQueueName is set, using native RMQDestination to create MessageConsumer.  queueName="+jmsProperties.queueName+", amqpQueueName="+amqpProperties.amqpQueueName);
				messageConsumer = jmsSession.createConsumer((Queue)(new RMQDestination(jmsProperties.queueName, null, null, amqpProperties.amqpQueueName)));
			}
			else {
				messageConsumer = jmsSession.createConsumer(jmsSession.createQueue(jmsProperties.queueName));
			}

			log.info("Registering listener for queue "+jmsProperties.queueName);
			messageConsumer.setMessageListener(simpleMessageListener);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Profile("subscribe")
	@Bean
	public MessageConsumer topicMessageConsumer(Session jmsSession, 
			                                    SimpleMessageListener simpleMessageListener, 
			                                    AMQPProperties amqpProperties,
			                                    JMSProperties jmsProperties) {
		MessageConsumer messageConsumer = null;
		try {
			Topic topic = null;
			if(amqpProperties.amqpQueueName != null && !"".equals(amqpProperties.amqpQueueName)) {
				log.info("rmqQueueName is set, using native RMQDestination to create Topic for MessageConsumer.  queueName="+jmsProperties.queueName+", amqpQueueName="+amqpProperties.amqpQueueName);
				topic = (Topic)(new RMQDestination(jmsProperties.topicName, null, null, amqpProperties.amqpQueueName));
			}
			else {
				topic = jmsSession.createTopic(jmsProperties.topicName);
			}
			
			if(!"not-durable".equals(jmsProperties.durableQueue)) {
				log.info("Creating durable queue for subscriber with name "+jmsProperties.durableQueue);
				messageConsumer = jmsSession.createDurableSubscriber(topic, jmsProperties.durableQueue);
			} else {
				log.info("Creating transient queue for subscriber");
				messageConsumer = jmsSession.createConsumer(topic);
			}

			System.out.println("Registering listener for topic " + jmsProperties.topicName);
			messageConsumer.setMessageListener(simpleMessageListener);
		} catch(Exception e) {
			e.printStackTrace();
			messageConsumer = null;
		}
		return messageConsumer;
	}

	@Bean
	public Session jmsPoisonResponseSession(Connection connection) {
		Session session = null;
		try {
			log.info("Creating session for poison message return queues (transacted=false, Session.AUTO_ACKNOWLEDGE)");
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		} catch (JMSException e) {
			e.printStackTrace();
			session = null;
		}
		return session;
	}

	@Bean
	public MessageProducer backoutQueueProducer(Session jmsPoisonResponseSession, AppProperties appProperties) {
		MessageProducer messageProducer = null;
		try {
			if(appProperties.poisonTryLimit>0) {
				log.info("Creating the backoutQueueProducer sending to queue="+appProperties.poisonBackoutQueue);
				messageProducer = jmsPoisonResponseSession.createProducer(jmsPoisonResponseSession.createQueue(appProperties.poisonBackoutQueue));
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return messageProducer;
	}
	
//	@Bean
//	public MessageProducer reQueueProducer(Session session,
//			                               AppProperties appProperties,
//			                               AMQPProperties amqpProperties,
//			                               JMSProperties jmsProperties) {
//
//		MessageProducer messageProducer = null;
//
//		String destName = amqpProperties.amqpExchangeName;		
//		if(destName == null) {
//			destName = jmsProperties.queueName;
//		}
//
//		try {
//			if(appProperties.poisonTryLimit>0) {
//				log.info("Creating the reQueueProducer sending to queue="+destName);
//				messageProducer = session.createProducer(session.createQueue(destName));
//			}
//		} catch (JMSException e) {
//			e.printStackTrace();
//		}
//		return messageProducer;
//	}
}
