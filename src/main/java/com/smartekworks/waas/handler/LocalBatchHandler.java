package com.smartekworks.waas.handler;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.ArrayList;

public class LocalBatchHandler extends AbstractHandler {
	private File basePath = null;
	private JSONObject commands = null;

	public LocalBatchHandler() {

	}

	public LocalBatchHandler(JSONObject properties) {
		if (properties.has("basePath")) {
			basePath = new File(properties.getString("basePath"));
			if (!basePath.exists() || !basePath.isDirectory()) {
				basePath = null;
			}
		}
	}

	public void prepare(JSONObject commands) {
		this.commands = commands;
	}

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		HashMap<String, Object> retMap = new HashMap<>();
		String charset = "UTF-8";
		JSONObject commands = this.commands;
		int httpStatus = HttpServletResponse.SC_OK;

		try {
			// get json from request
			StringBuffer jb = new StringBuffer();
			String line;
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null)
				jb.append(line);

			JSONObject jsonIn = new JSONObject(jb.toString());

			if (jsonIn.has("commandFile")) {
				File commandFile = basePath!=null?new File(basePath, jsonIn.getString("commandFile")):new File(jsonIn.getString("commandFile"));
				if (commandFile.exists() && !commandFile.isDirectory()) {
					commands = new JSONObject(FileUtils.readFileToString(commandFile, "UTF-8"));
				}
			}

			JSONObject command = commands.getJSONObject(jsonIn.getString("command"));

			String execResult = executeCommand(command, jsonIn);
			retMap.put("result", "executed");

			HashMap<String, String> evidence = new HashMap<>();
			evidence.put("name", "execResult.txt");
			evidence.put("type", "text/plain");
			byte[] base64Bytes = Base64.encodeBase64(execResult.getBytes());
			evidence.put("content", new String(base64Bytes));

			ArrayList<HashMap<String, String>> extraEvidences = new ArrayList<>();
			extraEvidences.add(evidence);
			retMap.put("extraEvidences", extraEvidences);
		} catch (Exception e) {
			e.printStackTrace();
			retMap.put("code", "EXECERROR");
			retMap.put("message", e.getMessage());
			httpStatus = HttpServletResponse.SC_NOT_FOUND;
		}

		JSONObject jsonOut = new JSONObject(retMap);

		response.setContentType("application/json;charset=" + charset);
		response.setStatus(httpStatus);
		baseRequest.setHandled(true);
		response.getWriter().write(jsonOut.toString());
	}

	private String executeCommand(JSONObject command, JSONObject values) throws Exception{
		String commandLine = command.getString("batch");
		JSONArray params = command.getJSONArray("params");
		for (int i = 0; i < params.length(); i++) {
			JSONObject param = params.getJSONObject(i);
			String value;
			if (values.isNull(param.getString("name"))) {
				value = param.getString("default");
			} else {
				value = values.getString(param.getString("name"));
			}
			commandLine += " " + value;
		}

		StringBuffer output = new StringBuffer();
		Process p;

		p = Runtime.getRuntime().exec(commandLine);
		p.waitFor();
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(p.getInputStream()));

		String line;
		while ((line = reader.readLine())!= null) {
			output.append(line + "\n");
		}

		return output.toString();

	}
}
