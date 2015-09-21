package com.smartekworks.waas;

import com.smartekworks.waas.handler.LocalBatchHandler;
import com.smartekworks.waas.handler.SimpleDBHandler;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class AgentServer {
	public static void main(String[] args) throws Exception
	{
		File confFile = new File("agent.conf");
		JSONObject config = new JSONObject(FileUtils.readFileToString(confFile, "UTF-8"));

		Server server = new Server(config.getInt("port"));

		ArrayList<Handler> handlers = new ArrayList<>();

		JSONArray modules = config.getJSONArray("modules");
		for (int i = 0; i < modules.length(); i++) {
			JSONObject module = modules.getJSONObject(i);
			if ("SimpleDBHandler".equals(module.getString("handler"))) {
				ContextHandler context = new ContextHandler(module.getString("path"));

				SimpleDBHandler handler = new SimpleDBHandler(module.getJSONObject("properties"));
				handler.prepare(module.getJSONObject("commands"));

				context.setHandler(handler);
				handlers.add(context);
			} else if ("LocalBatchHandler".equals(module.getString("handler"))) {
				ContextHandler context = new ContextHandler(module.getString("path"));

				LocalBatchHandler handler = new LocalBatchHandler(module.getJSONObject("properties"));
				handler.prepare(module.getJSONObject("commands"));

				context.setHandler(handler);
				handlers.add(context);
			}
		}

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		contexts.setHandlers(handlers.toArray(new Handler[handlers.size()]));

		server.setHandler(contexts);

		server.start();
		server.join();
	}
}
