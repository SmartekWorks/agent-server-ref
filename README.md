# agent-server

## Introduction

Agent Server is a customizable and extensible component which can interact with SWATHub server, and accomplish the customized operations such as database manipulation and local command execution.

## Build

Run `make.bat` or `make.sh` according to your operation systeam, and make sure `JAVA_HOME` and `MVN_HOME` is correctly set already.
Copy the `agent.conf` and startup scripts inside `release` folder into `target` folder, and then run `startup.bat` or `startup.sh`.

## Configuration

Please edit `agent.conf` to setup agent server modules. The configuration file is in JSON format which contains the following attributes:
* `port`: the port to which the agent server is listening
* `modules`: the list of agent server modules, and each module has the properties as below
 * `path`: the relative uri path
 * `handler`: the implementation of the module, and currently we provide two modules out-of-box: `SimpleDBHandler` in charge of query and update, and `LocalBatchHandler` to execute batch commands
 * `properties`: the map of module properties
 * `commands`: the list of commands supported in this module
 
Here is a sample snippet of `agent.conf`.
```
{
	"port":5555,
	"modules":[
		{
			"path":"/mysql",
			"handler":"SimpleDBHandler",
			"properties":{
				"driverName":"com.mysql.jdbc.Driver",
				"connString":"jdbc:mysql://localhost:3306/sample?useUnicode=yes&characterEncoding=UTF-8",
				"username":"user",
				"password":"pass"
			},
			"commands":{
				"getResultCount":{
					"sql":"select count(*) from result where status = ?",
					"params":[
						{
							"name":"status",
							"type":"string",
							"default":"success"
						}
					]
				}
			}
		}
	]
}
```

## Sample

### Update MySQL database

* Sample command configuration in `agent.conf` or external `command file`
```
	"updateResultStatus":{
		"sql":"update result set status = 'success' where id = ?",
		"params":[
			{
				"name":"id",
				"type":"integer",
				"default":1
			}
		]				
	}
```				
* Sample params for SWATHub System Operation `CallAPI`
 * URL: `http://localhost:5555/mysql/`, please keep in mind the last `/` is a **MUST**
 * Parameters: `command=updateResultStatus&id=8888`, if `id` is not set, the default value `1` will be used
 * Variable Name: `updateCount` for instance, and `@{updateCount}` could be referred in the later operations within the same **Test Scenario**
 
### Use external DB command files

* SQL commands can be stored in several external files and invoked in the test case execution run time. The `basePath` property should be set in this case. If `basePath` is not set, the commands inside `agent.conf` will be used instead.
```
	"properties":{
		"driverName":"com.mysql.jdbc.Driver",
		"connString":"jdbc:mysql://localhost:3306/sample?useUnicode=yes&characterEncoding=UTF-8",
		"username":"user",
		"password":"pass",
		"bathPath":"C:\\mySampleCommands"
	}
```
* Sample command file named `sampleCommand.json`
```
{
	"updateResultStatus":{
		"sql":"update result set status = 'success' where id = ?",
		"params":[
			{
				"name":"id",
				"type":"integer",
				"default":1
			}
		]				
	}	
}
```
* Sample params for SWATHub System Operation `CallAPI`
 * URL: `http://localhost:5555/mysql/`
 * Parameters: `commandFile=samplecommand.json&command=updateResultStatus&id=8888`
 * Variable Name: `updateCount` for instance

### List a directory in Windows
 
* Sample command configuration in `agent.conf`
```
 	"dir":{
		"batch":"C:\\Dev\\myWorkspace\\agent-server\\target\\dir.bat",
		"params":[
			{
				"name":"folder",
				"type":"string",
				"default":"c:\\"
			}
		]
	}
```
* Sample params for SWATHub System Operation `CallAPI`
  * URL: `http://localhost:5555/batch/`
  * Parameters: `command=dir&folder=c:\tmp`
  * Variable Name: better leave as blank, as the batch execution result would be saved as a file into the final evidence.
