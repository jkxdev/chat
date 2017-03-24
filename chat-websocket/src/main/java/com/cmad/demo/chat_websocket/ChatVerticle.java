package com.cmad.demo.chat_websocket;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;


public class ChatVerticle extends AbstractVerticle {
	
	static Vector<BufferHandler> webSocketHandlers = new Vector();
	static Vertx vertx = null;
	
	public void start(Future<Void> future) throws Exception {
		startChatServer("start", vertx);
	}

	public static void main(String[] args) {
		startChatServer("main", null);
	}
	
	private static void startChatServer(String str, Vertx vertxArg)	{
		System.out.println("starting...ChatVerticle on a different Vert.x from "+str);
		vertx = vertxArg;
		if(vertx == null)	{
			vertx = Vertx.vertx();
		}
//		printDefaultVerxtxOptionData();
		
		vertx.deployVerticle(ChatAuthenticationVerticle.class.getName(), new DeploymentOptions().setWorker(true));
		
//		vertx.createHttpServer().websocketHandler(new CustomWebSocketHandler()).listen(8080);
		System.out.println("ChatVerticle.startChatServer() listening @ localhost 8085");
		vertx.createHttpServer().websocketHandler(new CustomWebSocketHandler()).listen(8085);
	}
	
	public static void publishMessageToClients(String message) {
		Vector<BufferHandler> toBeRemovedWebSocketHandlers = new Vector();
		if (webSocketHandlers != null && webSocketHandlers.size() > 0) 
		{
			for (BufferHandler wsHandler : webSocketHandlers) 
			{
				try {
					wsHandler.getWebSocket().writeFinalTextFrame(message);
				}catch(IllegalStateException e)	{
					toBeRemovedWebSocketHandlers.add(wsHandler);
				}
			}
			if(toBeRemovedWebSocketHandlers.size() > 0)	{
				for (BufferHandler removeWsHandler : toBeRemovedWebSocketHandlers) 
				{
					webSocketHandlers.remove(removeWsHandler);
				}
				toBeRemovedWebSocketHandlers.removeAllElements();
			}
		}
	}
	
	static void printDefaultVerxtxOptionData()	{
		//////
		VertxOptions defVertxOptions = new VertxOptions();
		System.out.println("ChatVerticle.printDefaultVerxtxOptionData() VERTX DEFAULT OPTION DATA");
		
		System.out.println("----------------------------------------------------");
//		System.out.println("Runtime.getRuntime().availableProcessors() = "+Runtime.getRuntime().availableProcessors());
//		System.out.println("getEventLoopPoolSize() = "+defVertxOptions.getEventLoopPoolSize());
//		System.out.println("getWorkerPoolSize() = "+defVertxOptions.getWorkerPoolSize());
//		System.out.println("getInternalBlockingPoolSize() = "+defVertxOptions.getInternalBlockingPoolSize());
//		System.out.println("getMaxEventLoopExecuteTime() = "+defVertxOptions.getMaxEventLoopExecuteTime()+" ns, "+defVertxOptions.getMaxEventLoopExecuteTime()/1000000000+" seconds");
//		System.out.println("getMaxWorkerExecuteTime() = "+defVertxOptions.getMaxWorkerExecuteTime()+" ns, "+defVertxOptions.getMaxWorkerExecuteTime()/1000000000+" seconds");
//		System.out.println("getWarningExceptionTime() = "+defVertxOptions.getWarningExceptionTime()+" ns, "+defVertxOptions.getWarningExceptionTime()/1000000000+" seconds");
		System.out.println("----------------------------------------------------");
		
		System.out.println("BlockedThreadCheckInterval() = "+defVertxOptions.getBlockedThreadCheckInterval());
		System.out.println("ClusterHost() = "+defVertxOptions.getClusterHost());
		System.out.println("getClusterPingInterval() = "+defVertxOptions.getClusterPingInterval());
		System.out.println("getClusterPingReplyInterval() = "+defVertxOptions.getClusterPingReplyInterval());
		System.out.println("getClusterPort() = "+defVertxOptions.getClusterPort());
		System.out.println("getClusterPublicHost() = "+defVertxOptions.getClusterPublicHost());
		System.out.println("getClusterPublicPort() = "+defVertxOptions.getClusterPublicPort());
		System.out.println("getHAGroup() = "+defVertxOptions.getHAGroup());
		System.out.println("getQuorumSize() = "+defVertxOptions.getQuorumSize());
		System.out.println("getAddressResolverOptions() = "+defVertxOptions.getAddressResolverOptions());
		System.out.println("getClusterManager() = "+defVertxOptions.getClusterManager());
		System.out.println("getEventBusOptions() = "+defVertxOptions.getEventBusOptions());
		System.out.println("getMetricsOptions() = "+defVertxOptions.getMetricsOptions());
		//////		
	}
}

class CustomWebSocketHandler<E> implements Handler<E> {
	
	ServerWebSocket ws;
	
	public void handle(E event) {
		ws = (ServerWebSocket) event;
		System.out.println("CustomWebSocketHandler.handle() ws = "+ws);		
		////
		String idKey = "id";
		String tokenKey = "authToken";
		String query = ws.query();
		System.out.println("CustomWebSocketHandler.handle() query = "+query);		
		if(!query.contains(idKey))	{
			System.out.println("CustomWebSocketHandler.handle() Rejecting socket open as query does not contains userId");			
			ws.reject();
		}
		
		if(!query.contains(tokenKey))	{
			System.out.println("CustomWebSocketHandler.handle() Rejecting socket open as query does not contains auth token");			
			ws.reject();
		}
		
		if (!(ws.path().equals("/chat"))) {
			System.out.println("CustomWebSocketHandler.handle() Rejecting socket open as path does not contains word [chat]");			
			ws.reject();
		}
		
		String userId = query.substring(idKey.length()+1, query.indexOf(tokenKey)-1);
		String tokenValue = query.substring(query.indexOf(tokenKey)+tokenKey.length()+1);
		System.out.println("CustomWebSocketHandler.handle() ### idValue="+userId);
		System.out.println("CustomWebSocketHandler.handle() ### tokenValue="+tokenValue);

		postMsgForAuthentication(userId, tokenValue);
		
//		printWebSocketData(ws);
	}
	
	public void postMsgForAuthentication(String userId, String userToken) {

		Hashtable<String, Object> table = new Hashtable<String, Object>();
		table.put("userId", userId);
		table.put("userToken", userToken);
		
		JsonObject authJsonData =  new JsonObject(table);
		
		System.out.println("CustomWebSocketHandler.postMsgForAuthentication() authJsonData = "+authJsonData);
		DeliveryOptions deliveryOps = new DeliveryOptions();
		deliveryOps.setSendTimeout(600*1000*6);//setting timeout of 600sec*6(60mins)
		ChatVerticle.vertx.eventBus().send(ChatTopics.VALIDATE_TOKEN, authJsonData, deliveryOps, r -> {
			
//			System.out.println("CustomWebSocketHandler.postMsgForAuthentication() r = "+r);
//			if(r != null)
//				System.out.println("CustomWebSocketHandler.postMsgForAuthentication() r.result() = "+r.result());
//			if(r != null && r.result() != null)
//				System.out.println("CustomWebSocketHandler.postMsgForAuthentication() r.result().body() = "+r.result().body());

			boolean isValidUser = false;
			if(r != null && r.result() != null && r.result().body() != null)	{
				if(r.result().body().toString().equals("valid"))	{
					isValidUser = true;
				}
			}
			
			if (isValidUser) {
				BufferHandler<Buffer> bufferHandler = new BufferHandler<Buffer>(ws);
				ws.handler(bufferHandler);
				ChatVerticle.webSocketHandlers.add(bufferHandler);
				System.out.println("CustomWebSocketHandler.postMsgForAuthentication() webSocketHandlers.size() = "+ChatVerticle.webSocketHandlers.size());
				
				ws.closeHandler(new Handler<Void>() {
					@Override
					public void handle(final Void event) {
						ChatVerticle.webSocketHandlers.remove(bufferHandler);
					}
				});
			}
			else {
				System.out.println("CustomWebSocketHandler.postMsgForAuthentication() Rejecting socket open as authentication failed");
				try {
					ws.reject();
				} catch(IllegalStateException e)	{
					System.out.println("CustomWebSocketHandler.postMsgForAuthentication() IllegalStateException on reject");
				}
				try {
					ws.close();
				} catch(IllegalStateException e)	{
					System.out.println("CustomWebSocketHandler.postMsgForAuthentication() IllegalStateException on close");
				}
			}
		});
	}
	
	private void printWebSocketData(ServerWebSocket ws)	{
		System.out.println("CustomWebSocketHandler.printWebSocketData() path= "+ws.path());
		System.out.println("CustomWebSocketHandler.printWebSocketData() query= "+ws.query());
		System.out.println("CustomWebSocketHandler.printWebSocketData() binaryHandlerID= "+ws.binaryHandlerID());
		System.out.println("CustomWebSocketHandler.printWebSocketData() textHandlerID= "+ws.textHandlerID());
		System.out.println("CustomWebSocketHandler.printWebSocketData() uri= "+ws.uri());
		MultiMap headers = ws.headers();
		if(headers != null)	{
			Iterator headerIterator = headers.iterator();
			Object nextHeader;
			System.out.println("CustomWebSocketHandler.printWebSocketData() $$$$$$ HEADERS STARTS");
			while(headerIterator.hasNext())	{
				nextHeader = headerIterator.next();
				System.out.println("CustomWebSocketHandler.printWebSocketData() nextHeader = "+nextHeader);
			}
		}
	}
}

class BufferHandler<E> implements Handler<E>	{
	
	ServerWebSocket ws;
	int[] intArr;
	String message;
	static int counter = 0;
	
	public BufferHandler(ServerWebSocket webSocket) {
		this.ws = webSocket;
	}
	
	public ServerWebSocket getWebSocket() {
		return ws;
	}
	
	public void handle(E event) {
		System.out.println("BufferHandler.handle() event = "+event.getClass());
		System.out.println("BufferHandler.handle() ((Buffer)event).toString() = "+((Buffer)event).toString());

		message = ((Buffer)event).toString();
		counter++;
		ChatVerticle.publishMessageToClients(" MsgNumber: "+counter+" "+message);
	}
	
}