# Retz release notes

## 0.0.30

* Fix a bug (#38) where files and directories in a directory in the sandbox
  cannot be downloaded or listed. This introduces *incompatible changes*
  to the client-server protocol. Also, options of "list-files" and 'get-file'
  are changed.
* 'localhost', '127.0.0.1' and '0.0.0.0' are officially not allowed as
  a value of 'retz.mesos' in Retz server configuration.
* Add port range allocation which is supported by Mesos - available at
  'run' and 'schedule' subcommand with '-ports' option specifying *amount*
  of ports, which will be provided as environment variables '$PORT0',
  '$PORT1' and so on. All jobs may use up to 1000 ports. This introduces
  *incompatible changes* to JSON formatting over network.
* Split FileConfiguration to client and server configuration, guiding
  more suitable key names and descriptions. Especially, 'retz.bind' at
  client side config is renamed to 'retz.server.uri'.

## 0.0.29

* Fix wrong timestamp representaion in all logs; it should be 0-23 hours
  for a day but it was 0-12 without AM/PM notion.
* Fix a bug where an exceeding job is to be launched with stocked offers,
  with a huge clean up on creating execution plan matching offers and jobs
* Print reason at 'retz-client run' when the job is killed
* Remove Persistent Volumes support

## 0.0.28

* Fix a bug where return value of 'retz-client run' always being non-zero
  (esp. default value of Job.result()). By the way, since 0.0.27, Retz is
  using Mesos' default command executor and thus Retz cannot take return
  value of application command any more. Instead it returns `0` iff the
  task state ends with `TASK_FINISHED`, otherwise `i - TASK_FINISHED` where
  `i` is the last task state.
* Update HTML page '/' removing all WebSocket JavaScript code and replace
  with simple guidance and small update on latest status.
* Remove 'watch' command and protocol. This changed 'ScheduleRequest'
  protocol and introduced incompatibility.
* Add integer priority field to 'Job' object in the protocol for future use.
  This introduces incompatible change to client-server protocol.
* Preserve Framework id in database and reuse it after restart. Mesos
  scheduler failover timeout is now set as 1 week (hard coded). Now
  Retz has at most one week maintenance window without losing any
  information or tasks. After 1 week absent if framework id is
  preserved in database, Mesos will refuse connection from Retz unless
  Retz forgets the framework id. In that case, just removing framework
  id from database (i.e. deleting whole database file, or executing
  `DELETE FROM properties WHERE key='FrameworkID'` at database
  console) will enable Retz connect to Mesos again.
* Add PostgreSQL JDBC driver with configuration items such as
  'retz.database.driver', 'retz.database.user' and
  'retz.database.pass' to support PostgreSQL as backend
  database. Currently it only support database name 'retz'.
* Remove H2 connection pool and use Tomcat JDBC connection pool.

## 0.0.27

* Add '-stderr' option to 'retz-client run' so that job standard error is
  easily printed at the client console, which is off by default.
* Remove RetzExecutor, preserving as local mode executor. Pre-set environment
  variable including `$RETZ_CPU`, `$RETZ_MEM` and `$RETZ_PVNAME` are now
  removed. Instead, reuse arguments of `-cpu`, `-mem` and persistent volume
  name.
* Remove 'unload-app' and add 'enabled' flag to application instead, and
  made application overwritable. In near future, Namely `UnloadApp`
  protocol, 'DELETE /app/<appid>' will be deprecated and removed. Also,
  this introduces in incompatible change to client-server protocol.
* Add administration CLI tool 'retz-admin' as a new subproject, with JMX
  ports opening at startup. With 'retz-admin', many features like creating,
  disabling and getting information of users get available via JMX local
  port. It will be released as another release package as well as
  'retz-server' and 'retz-client'. 
* Remove logback from client to avoid initialization overhead
* Change default value of 'retz.max.running' from 512 to 128
* Add Docker volume mount option and protocol. By adding 
  `--docker-volumes Vol1,Vol2,..` to 'load-app', subcommand on Docker
  container, jobs on all those applications will be with those volumes
  mounted inside Docker containers. Besides, Docker volume driver process
  must be running all Mesos Agent nodes.
* Add shutdown hook to gracefully and safely stop database
* Add resource offer stocking with limit `retz.max.stock` in configuration.
  With offers stocked in scheduler and job queue empty, a new job schedule
  or run request do not need to wait for resource offer - task will be
  invoked during the schedule request. Scheduler may stock up to `retz.max.stock`
  (default 16) offers. To disable stocking, set it 0. Hit `/status` API to
  see current status of offers.
* Remove Range from CUI and protocol
* Major clean up moving from on-memory data structure utilizing java.util.concurrent
  to data structure based on relational model and transactions using JDBC.
  Currently primary database is H2 Database - on moving to other database
  it will require change on connection pooling.

## 0.0.26

* Add 'retz.exectutor.java' to server configuration file for more flexible
  Java installation on Mesos Agents. `TASK_FAILED` and log `Message from
  Mesos executor: Executor terminated:REASON_EXECUTOR_TERMINATED` in Retz
  server may indicate correct Java could not be found.
* Add 'retz.max.running' to server configuration file. This number limits
  number of simultaneously running jobs, to control Retz load to whole
  Mesos cluster.
* Add 'Server:' header into response from Server to know right version
  of client to use.
* Implement framework authentication by adding 'retz.mesos.secret.file'
  to server configuration file. This file gives Retz a creditial to
  connect Mesos master.

## 0.0.25

* Add '-U' and '--user' option to 'load-app' to change user name in
  executor; sometimes it is convenient to run as root. Besides, it is
  not allowed to be root in RetzExecutor.
* Move from WebSocket to HTTP polling at 'run' subcommand.
* Add 'get-file' and 'list-files' subcommand.
* Simplify 'get-job' as to get summary of a job; getting stdout and stderr
  is moved to 'get-file'.
* Remove '-R' option from 'run' subcommand, instead it prints stdout of
  a remote job to local standard output. This is based on polling, as it
  may be replaces with combination of 'schedule' and 'get-file' with '--poll'
  option. Currently, polling interval starts with 1 second and increases
  up to 32 seconds unless any update comes up. Any update will reset the
  interval to initial interval.
* Remove direct communication between clients and Mesos servers, which is
  now relayed by Retz server with `GetFileRequest` and `ListFilesRequest`
  protocol - now Retz clients only talks with Retz server via single
  HTTP(S) route.
* Updates from Mesos master on Task status are more up to date on states
  managed inside Retz server - which still does not have complete
  correspondence.
* Simple client-server authentication support: only for single user.
  As a special exception, commands that use WebSocket connection does
  not require authentication (it just does not work). Authentication is
  enabled by default. To secure application usage, Set your own pair of
  `retz.access.key` and `retz.access.secret`, which accepts arbitrary ASCII
  string. To disable authentication, in both client and server, just
  explicitly set `retz.authentication = false`. 
* Remove too strong message at client on accessing server with HTTP.

## 0.0.24

* Add TLS to connection between client and server. Make sure that the
  server is using valid certificate, or Mesos fetcher cannot fetch
  RetzExecutor jar file and no job cannot be run. In that case only
  Docker executor may work (use `--container docker` to load application).
  (Note: when TLS enabled 'run' and 'watch' does not work well as it is
  still using 'ws://' protocol)
* Change type of `-gpu` argument of CLI from range to integer
* Replace Docker containerizer with unified containerizer using Docker:
  this changes Mesos Agent setup requirement as to add
  `--image_providers=docker` and
  `--isolation=docker/runtime,filesystem/linux` in addition to existing
  configuration. See Mesos document [on containers](https://github.com/apache/mesos/blob/master/docs/container-image.md)
  and [on GPU with Docker](https://github.com/apache/mesos/blob/master/docs/gpu-support.md#minimal-setup-with-support-for-docker-containers)

## 0.0.23

* Update Mesos library to 1.0.1
* Add 'kill' subcommand and protocol to kill a job
* Add 'get-app' subcommand and protocol to get application details
* Add Docker image execution support
* Improve WebSocket disconnect handling

## 0.0.22

* Add server side handler on WebSocketException

## 0.0.21

* Add workaround where a NullPointerException kills the whole service

## 0.0.20

* Change all WebSocket communication asynchronous to prevent Mesos scheduler
  thread callback from being blocked by WebSocket communication, especially
  sending broadcast message from server to clients

## 0.0.19

* Add retry in case of task got LOST
* Add new '-L' option on `load-app`: '-F' used to require agents to cache
  files, but it is replaced by '-L' ('--large-file'), and '-F' is for
  files that shouldn't be cached (like program files for try-'n-error!)

## 0.0.18

* Retry of compromised 0.0.17 release

## 0.0.17 (not release and no tag)

* Migrate most of RPCs on WebSocket to plain HTTP request-response style

## 0.0.16

* This is another release for debug build: log output of server and client
  are both in debug level.
* Fix a bug that environment variables on 'run' and 'schedule' which
  includes '=' in values were ignored. This bug has been resident since
  0.0.13.
* By cleaning up messaging from executor to scheduler, several messages
  from executors does not generate unnecessary exception any more

## 0.0.15

* This is a release for debug build: log output of server and client are
  both in debug level.
* Several debug log around WebSocket PING/PONG is added
* Add 'help' subcommand to client CLI

## 0.0.14

* Fix several stacktrace printing from stderr to log stream
* Change state representation at 'list' command
* Fix log output at assigned resouces on an offer

## 0.0.13 (not released and no tag)

* Introduce [Wercker](https://app.wercker.com/retz/retz/) as external
  CI service to keep sanity
* Fix exception in server side when Mesos sends message to scheduler
* Fix ordering of retz-client subcommand: '-C path/to/retz.properties'
  **must** be before subcommand, otherwise it fails, by moving to
  JCommander in client

## 0.0.12

* Improve output format of the result of 'list', aligning and duration
* Fix return value of 'get-job' success

## 0.0.11

* Change output format of the result of 'list' and sort it by Job id
* Fix bug keepalive interval being longer than Jetty idle timeout
* Support single range format of just a number like "-4", "-32" for
  CLI options like'-cpu', '-mem', '-disk', and '-gpu'

## 0.0.10

* Add job history compaction when it exceeds max payload size in client
* Add 'get-job' subcommand to CLI
* Add running and finished jobs to the result of 'list' subcommand
* Add GPU support

## 0.0.9

* Squash all past commits to remove confidential information

## 0.0.8

* Add retz.mesos.offer_interval as retz.config parameter to control
  resource offer interval
* Update to Mesos 1.0.0
* Show standard error/ouput regardless of job status

## 0.0.7

* Fix race condition on `run` subcommand in client side
* Add Apache License 2.0 header to all files
* Show standard error to console on `run`
* Remove log on every resource offer, but only at accepted time
* Add `-trustpvfiles` option to Client CLI to omit decompression cost

## 0.0.6

* Add persistent volumes support with `-P` options on `load-app`
  with preservation of extracted tarballs there
* Directly show stdout result by specifying `-R -`
* Add preset environment variables `RETZ_CPU`, `RETZ_MEM` and
  `RETZ_PVNAME` at executor on spawning application command
* Fix CLI block forever when an command execution fails
* Update Gradle version to 2.14.1
* Add basic test code to integration test with real Mesos system
  in docker image
