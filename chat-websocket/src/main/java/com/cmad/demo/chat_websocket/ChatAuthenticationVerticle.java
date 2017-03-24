package com.cmad.demo.chat_websocket;

import java.io.IOException;
import java.util.Hashtable;

import com.cmad.demo.rabbitmq.messaging.RPCRabbitMqClient;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;

/*
 * Takes care of handing user authentication validation before allowing user to open a web socket
 */
public class ChatAuthenticationVerticle extends AbstractVerticle {

	static Hashtable<AuthData, Message> authHandlingTable = new Hashtable<AuthData, Message>(); 
	
	@Override
	public void start() throws Exception {
		handleAuthentication();
	}

	private void handleAuthentication() {
		vertx.eventBus().consumer(ChatTopics.VALIDATE_TOKEN, message -> {
			System.out.println("ChatAuthenticationVerticle.handleAuthentication() message = "+message);
			System.out.println("ChatAuthenticationVerticle.handleAuthentication() message.body() = "+message.body());
			AuthData authData = Json.decodeValue(message.body().toString(), AuthData.class);

			System.out.println("ChatAuthenticationVerticle.handleAuthentication() authData = " + authData);
			
			try {
				RPCRabbitMqClient rabbitMqClient = new RPCRabbitMqClient(authData);
				authHandlingTable.put(authData, message);
				rabbitMqClient.validateToken();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		});
	}
	
	public static void sendResponse(AuthData authData, String response)	{
		System.out.println("ChatAuthenticationVerticle.sendResponse() authData = "+authData);
		if(authHandlingTable != null)	{
			Message message = authHandlingTable.get(authData);
			authHandlingTable.remove(authData);
			if(message != null)	{
				if (response == null) {
					message.fail(401, "Not a valid User");
				} else {
					message.reply(response);
				}
			}
		}
	}
}
