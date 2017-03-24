package com.cmad.demo.rabbitmq.messaging;

import java.io.IOException;
import java.util.UUID;

import com.cmad.demo.chat_websocket.AuthData;
import com.cmad.demo.chat_websocket.ChatAuthenticationVerticle;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import io.vertx.core.VertxException;
import io.vertx.core.json.Json;

public class RPCRabbitMqClient {

	  private Connection connection;
	  private Channel channel;
	  private String requestQueueName = "rpc_queue_blog_auth";
	  private String replyQueueName;
	  private String replyMsg = "";
	  private AuthData authData;

	  public RPCRabbitMqClient(AuthData authData) throws IOException {
		this.authData = authData;
	    ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost("localhost");

	    connection = factory.newConnection();
	    channel = connection.createChannel();

	    replyQueueName = channel.queueDeclare().getQueue();
//System.out.println("RPCRabbitMqClient.RPCRabbitMqClient() replyQueueName = "+replyQueueName);
	  }

	private void call(String message) throws IOException, InterruptedException {
		final String corrId = UUID.randomUUID().toString();
//		System.out.println("RPCRabbitMqClient.call() corrId = " + corrId);
//		System.out.println("RPCRabbitMqClient.call() replyQueueName = " + replyQueueName);
		AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().correlationId(corrId).replyTo(replyQueueName)
				.build();
//		System.out.println("RPCRabbitMqClient.call() sending message on requestQueueName = " + requestQueueName);
//		System.out.println("RPCRabbitMqClient.call() message.getBytes(UTF-8) = " + message.getBytes("UTF-8"));
		channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

		channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
					byte[] body) throws IOException {
				System.out.println("RPCRabbitMqClient.handleDelivery() GOT RESPONSE body = " + body);
//				System.out.println("RPCRabbitMqClient.handleDelivery() consumerTag = " + consumerTag);
//				System.out.println("RPCRabbitMqClient.handleDelivery() properties.getCorrelationId() = "
//						+ properties.getCorrelationId());
//				System.out.println("RPCRabbitMqClient.handleDelivery() corrId = " + corrId);
				if (properties.getCorrelationId().equals(corrId)) {
					System.out.println("RPCRabbitMqClient.handleDelivery() corrId is matching");
//					response.offer(new String(body, "UTF-8"));
					replyMsg =  new String(body, "UTF-8");
//					close();// channel should never be closed.
					ChatAuthenticationVerticle.sendResponse(authData, replyMsg);
				}
			}
		});
	}

	private void close() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void validateToken() {
		try {
			String authDataMsg = Json.encodePrettily(authData);
			System.out.println("RPCRabbitMqClient.validateToken() authDataMsg = " + authDataMsg);
			call(authDataMsg);
		} catch (IOException e) {
			System.out.println("RPCRabbitMqClient.validateToken() IOException e="+e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("RPCRabbitMqClient.validateToken() InterruptedException e="+e.getMessage());
			e.printStackTrace();
		} catch (VertxException e) {
			System.out.println("RPCRabbitMqClient.validateToken() VertxException e="+e.getMessage());
			e.printStackTrace();
		} /*finally {
			System.out.println("RPCRabbitMqClient.validateToken() Got " + response);
			close();
		}*/
	}
}
