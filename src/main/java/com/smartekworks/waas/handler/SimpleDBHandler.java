package com.smartekworks.waas.handler;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.dbcp2.BasicDataSource;
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
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

public class SimpleDBHandler extends AbstractHandler {

	private BasicDataSource ds = new BasicDataSource();
	File basePath = null;
	private JSONObject commands = null;

	public SimpleDBHandler() {
	}

	public SimpleDBHandler(JSONObject properties) {
		try {
			String connectionURL = properties.getString("connString");
			Class.forName(properties.getString("driverName"));
			// Create string of connection url within specified format with machine name, port number and database name.
			ds.setDriverClassName(properties.getString("driverName"));
			ds.setUsername(properties.getString("username"));
			ds.setPassword(properties.getString("password"));
			ds.setUrl(connectionURL);
			if (properties.has("basePath")) {
				basePath = new File(properties.getString("basePath"));
				if (!basePath.exists() || !basePath.isDirectory()) {
					basePath = null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void prepare(JSONObject commands) {
		this.commands = commands;
	}

	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		LinkedHashMap<String, Object> retMap = new LinkedHashMap<>();
		String charset = "UTF-8";
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
				File commandFile = basePath==null?new File(basePath, jsonIn.getString("commandFile")):new File(jsonIn.getString("commandFile"));
				if (commandFile.exists() && !commandFile.isDirectory()) {
					commands = new JSONObject(FileUtils.readFileToString(commandFile, "UTF-8"));
				}
			}

			JSONObject command = commands.getJSONObject(jsonIn.getString("command"));

			List<LinkedHashMap<String, Object>> execResult = executeSql(command, jsonIn);

			// set result back to response
			if (execResult.size() > 0) {
				Entry<String, Object> firstEntry = execResult.get(0).entrySet().iterator().next();
				Object firstValue = firstEntry.getValue();
				retMap.put("result", firstValue.toString());

				LinkedHashMap<String, String> evidence = new LinkedHashMap<>();
				evidence.put("name", "queryResult.txt");
				evidence.put("type", "text/plain");

				JSONArray jsonResult = new JSONArray(execResult);
				byte[] base64Bytes = Base64.encodeBase64(jsonResult.toString().getBytes());
				evidence.put("content", new String(base64Bytes));

				ArrayList<LinkedHashMap<String, String>> extraEvidences = new ArrayList<>();
				extraEvidences.add(evidence);
				retMap.put("extraEvidences", extraEvidences);
			}
		} catch (Exception e) {
			retMap.put("code", "DBERROR");
			retMap.put("message", e.getMessage());
			httpStatus = HttpServletResponse.SC_NOT_FOUND;
		}

		JSONObject jsonOut = new JSONObject(retMap);

		response.setContentType("application/json;charset=" + charset);
		response.setStatus(httpStatus);
		baseRequest.setHandled(true);
		response.getWriter().write(jsonOut.toString());
	}

	private List<LinkedHashMap<String, Object>> executeSql(JSONObject command, JSONObject values) throws SQLException {
		List<LinkedHashMap<String, Object>> retValue = new ArrayList<>();

		Connection connection = ds.getConnection();
		String queryString = command.getString("sql");
		JSONArray params = command.getJSONArray("params");
		PreparedStatement statement = connection.prepareStatement(queryString);
		for (int i = 0; i < params.length(); i++) {
			JSONObject param = params.getJSONObject(i);
			if ("string".equals(param.getString("type"))) {
				String value;
				if (values.isNull(param.getString("name"))) {
					value = param.getString("default");
				} else {
					value = values.getString(param.getString("name"));
				}
				statement.setString(i + 1, value);
			} else if ("integer".equals(param.getString("type"))) {
				int value;
				if (values.isNull(param.getString("name"))) {
					value = param.getInt("default");
				} else {
					value = values.getInt(param.getString("name"));
				}
				statement.setInt(i + 1, value);
			}
		}

		// sql query to retrieve values from the specified table.
		if (queryString.trim().toLowerCase().startsWith("select")) {
			ResultSet rs = statement.executeQuery();
			retValue = resultSetToArrayList(rs);
			rs.close();
		} else {
			int updateQuery = statement.executeUpdate();
			if (updateQuery == 0) {
				throw new SQLException("executeUpdate error!");
			} else {
				LinkedHashMap<String, Object> updateResult = new LinkedHashMap<>();
				updateResult.put("updateResult", updateQuery);
				retValue.add(updateResult);
			}
		}

		statement.close();
		connection.close();

		return retValue;
	}

	private List<LinkedHashMap<String, Object>> resultSetToArrayList(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		ArrayList<LinkedHashMap<String, Object>> list = new ArrayList<>();
		while (rs.next()){
			LinkedHashMap<String, Object> row = new LinkedHashMap<>(columns);
			for(int i = 1; i <= columns; i++){
				row.put(md.getColumnName(i),rs.getObject(i));
			}
			list.add(row);
		}

		return list;
	}

}
