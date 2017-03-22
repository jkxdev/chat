package com.cmad.demo.chat_websocket;

import java.util.Iterator;
import java.util.Vector;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;


public class ChatVerticle extends AbstractVerticle {
	
	static Vector<BufferHandler> webSocketHandlers = new Vector();

	public void start(Future<Void> future) throws Exception {
		startChatServer("start", vertx);
	}

	public static void main(String[] args) {
		startChatServer("main", null);
	}
	
	private static void startChatServer(String str, Vertx vertxArg)	{
		System.out.println("starting...ChatVerticle on a different Vert.x from "+str);
		Vertx vertx = vertxArg;
		if(vertx == null)	{
			vertx = Vertx.vertx();
		}
		
//		vertx.createHttpServer().websocketHandler(new CustomWebSocketHandler()).listen(8080);
		System.out.println("ChatVerticle.startChatServer() listening @ localhost 8085");
		vertx.createHttpServer().websocketHandler(new CustomWebSocketHandler()).listen(8085);
	}
	
	public static void publishMessageToClients(String message) {
		if (webSocketHandlers != null && webSocketHandlers.size() > 0) 
		{
			for (BufferHandler wsHandler : webSocketHandlers) 
			{
				wsHandler.getWebSocket().writeFinalTextFrame(message);
			}
		}
	}
}

class CustomWebSocketHandler<E> implements Handler<E> {
	
	public void handle(E event) {
		ServerWebSocket ws = (ServerWebSocket) event;
		
		////
		String idKey = "id";
		String tokenKey = "authToken";
		String query = ws.query();
		if(!query.contains(idKey) || !query.contains(tokenKey))
			ws.reject();
//		if(!query.startsWith(tokenKey))
//			ws.reject();
		String idValue = query.substring(tokenKey.length()+1, query.indexOf(tokenKey)-1);
		String tokenValue = query.substring(tokenKey.length()+1);
		System.out.println("CustomWebSocketHandler.handle() ### idValue="+idValue);
		System.out.println("CustomWebSocketHandler.handle() ### tokenValue="+tokenValue);
		////
		
		printWebSocketData(ws);
		
		if (ws.path().equals("/chat")) {

			BufferHandler<Buffer> customHandler = new BufferHandler<Buffer>(ws);
			ws.handler(customHandler);
			ChatVerticle.webSocketHandlers.add(customHandler);
			System.out.println("WebSocketHandler.handle() webSocketHandlers.size() = "+ChatVerticle.webSocketHandlers.size());
			
			ws.closeHandler(new Handler<Void>() {
				@Override
				public void handle(final Void event) {
					ChatVerticle.webSocketHandlers.remove(customHandler);
				}
			});
		} else {
			ws.reject();
		}
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