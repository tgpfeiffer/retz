/**
 *    Retz
 *    Copyright (C) 2016 Nautilus Technologies, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.retz.protocol.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import static io.github.retz.protocol.data.Job.JobState.*;

public class Job {
    private final String cmd;
    private String scheduled;
    private String started;
    private String finished;
    private Properties props;
    private int result = -1;

    private int id;
    private String url;
    private String reason;
    private int retry; // How many retry now we have
    private int priority;

    private final String appid;
    private String name; // TODO: make this configurable;
    private final int cpu;
    private final int memMB;
    private int gpu;
    private final int diskMB; // Number of disk quota the job requires
    private int ports; // Number of ports the job requires

    private String taskId; // TaskId assigned by Mesos (or other scheduler)

    /**
     * State diagram:
     *  [CREATED] ---&gt; [QUEUED] ---&gt; [STARTED] ---&gt; [FINISHED]
     *                    |              +--------&gt; [KILLED]
     *                    +----------------------------^
     */
    public enum JobState { // TODO: define correspondce against Mesos Task status
        CREATED,
        QUEUED,
        STARTING,
        STARTED,
        FINISHED,
        KILLED,
    }

    private JobState state;

    private boolean trustPVFiles = false;

    public Job(String appName, String cmd, Properties props, int cpu, int memMB) {
        this.appid = appName;
        this.cmd = cmd;
        this.props = props;
        assert cpu > 0 && memMB >= 32;
        this.cpu = cpu;
        this.memMB = memMB;
        this.gpu = 0;
        this.diskMB = 0;
        this.ports = 0;
        this.state = CREATED;
        this.retry = 0;
        this.priority = 0;
    }

    public Job(String appName, String cmd, Properties props, int cpu, int memMB, int gpu, int ports) {
        this(appName, cmd, props, cpu, memMB);
        this.gpu = Objects.requireNonNull(gpu);
        this.ports = ports;
    }

    @JsonCreator
    public Job(@JsonProperty(value = "cmd", required = true) String cmd,
               @JsonProperty("scheduled") String scheduled,
               @JsonProperty("started") String started,
               @JsonProperty("finished") String finished,
               @JsonProperty("env") Properties props,
               @JsonProperty("result") int result,
               @JsonProperty(value = "id", required = true) int id,
               @JsonProperty("url") String url,
               @JsonProperty("reason") String reason,
               @JsonProperty("retry") int retry,
               @JsonProperty("priority") int priority,
               @JsonProperty(value = "appid", required = true) String appid,
               @JsonProperty(value = "name") String name,
               @JsonProperty(value = "cpu", required = true) int cpu,
               @JsonProperty(value = "memMB", required = true) int memMB,
               @JsonProperty("gpu") int gpu,
               @JsonProperty("diskMB") int diskMB,
               @JsonProperty("ports") int ports,
               @JsonProperty("taskId") String taskId,
               @JsonProperty("trustPVFiles") boolean trustPVFiles,
               @JsonProperty("state") JobState state) {
        this.cmd = Objects.requireNonNull(cmd);
        this.scheduled = scheduled;
        this.started = started;
        this.finished = finished;
        this.props = props;
        this.result = result;
        this.id = Objects.requireNonNull(id);
        this.url = url;
        this.reason = reason;
        this.retry = retry;
        this.priority = priority;
        this.appid = appid;
        this.name = name;
        assert cpu > 0;
        this.cpu = cpu;
        assert memMB >= 32;
        this.memMB = memMB;
        this.diskMB = diskMB;
        this.ports = ports;
        this.gpu = gpu;
        this.taskId = taskId;
        this.trustPVFiles = trustPVFiles;
        this.state = Objects.requireNonNull(state);
    }

    @JsonGetter("cmd")
    public String cmd() {
        return cmd;
    }

    @JsonGetter("scheduled")
    public String scheduled() {
        return scheduled;
    }

    @JsonGetter("started")
    public String started() {
        return started;
    }

    @JsonGetter("props")
    public Properties props() {
        return props;
    }

    @JsonGetter("finished")
    public String finished() {
        return finished;
    }

    @JsonGetter("result")
    public int result() {
        return result;
    }

    @JsonGetter("id")
    public int id() {
        return id;
    }

    @JsonGetter("url")
    public String url() {
        return url;
    }

    @JsonGetter("reason")
    public String reason() {
        return reason;
    }

    @JsonGetter("retry")
    public int retry() {
        return retry;
    }

    @JsonGetter("priority")
    public int priority() {
        return priority;
    }

    @JsonGetter("appid")
    public String appid() {
        return appid;
    }

    @JsonGetter("name")
    public String name() {
        return name;
    }

    @JsonGetter("cpu")
    public int cpu() {
        return cpu;
    }

    @JsonGetter("memMB")
    public int memMB() {
        return memMB;
    }

    @JsonGetter("gpu")
    public int gpu() {
        return gpu;
    }

    @JsonGetter("diskMB")
    public int diskMB() {
        return diskMB;
    }

    @JsonGetter("ports")
    public int ports() {
        return ports;
    }

    @JsonGetter("taskId")
    public String taskId() {
        return taskId;
    }

    @JsonGetter("trustPVFiles")
    public boolean trustPVFiles() {
        return trustPVFiles;
    }

    @JsonGetter("state")
    public JobState state() {
        return state;
    }

    public void setTrustPVFiles(boolean trustPVFiles) {
        this.trustPVFiles = trustPVFiles;
    }

    public void schedule(int id, String now) {
        this.id = id;
        this.scheduled = now;
        this.state = QUEUED;
    }

    public void doRetry() {
        this.state = QUEUED;
        this.retry++;
    }

    public void starting(String taskId, Optional<String> maybeUrl, String now) {
        this.started = now;
        this.taskId = taskId;
        if (maybeUrl.isPresent()) {
            this.url = maybeUrl.get();
        }
        this.state = STARTING;
    }

    public void started(String taskId, Optional<String> maybeUrl, String now) {
        this.started = now;
        this.taskId = taskId;
        if (maybeUrl.isPresent()) {
            this.url = maybeUrl.get();
        }
        this.state = STARTED;
    }

    public void finished(String now, Optional<String> url, int result) {
        this.finished = now;
        this.result = result;
        if (url.isPresent()) {
            this.url = url.get();
        }
        this.state = FINISHED;
    }

    public void killed(String now, Optional<String> maybeUrl, String reason) {
        this.finished = now;
        this.reason = reason;
        if (maybeUrl.isPresent()) {
            this.url = maybeUrl.get();
        }
        this.state = KILLED;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{")
                .append("id=").append(id)
                .append(", name=").append(name)
                .append(", appid=").append(appid)
                .append(", cmd=").append(cmd)
                .append(", env=").append(props)
                .append(", cpus=").append(cpu)
                .append(", mem=").append(memMB);

        if (gpu > 0) {
            sb.append(", gpu=").append(gpu);
        }
        if (diskMB > 0) {
            sb.append(", disk=").append(diskMB);
        }
        if (ports > 0) {
            sb.append(", ports=").append(ports);
        }
        if (scheduled != null) {
            sb.append(", scheduled=").append(scheduled);
        }
        if (started != null) {
            sb.append(", started=").append(started);
        }
        if (finished != null) {
            sb.append(", finished=").append(finished);
        }
        sb.append(", state=").append(state);
        if (state == FINISHED) {
            sb.append(", result=").append(result);
        }
        if (result != 0) {
            sb.append(", reason=").append(reason);
        }
        sb.append(", taskid=").append(taskId)
                .append("}");
        return sb.toString();
    }

    public String pp() {
        StringBuilder sb = new StringBuilder("{")
                .append("id=").append(id)
                .append(", name=").append(name)
                .append(", appid=").append(appid)
                .append(", cmd=").append(cmd)
                .append(", env=").append(props)
                .append(", cpus=").append(cpu)
                .append(", mem=").append(memMB);

        if (gpu > 0) {
            sb.append(", gpu=").append(gpu);
        }
        if (diskMB > 0) {
            sb.append(", disk=").append(diskMB);
        }
        if (ports > 0) {
            sb.append(", ports=").append(ports);
        }
        if (scheduled != null) {
            sb.append(", scheduled=").append(scheduled);
        }
        if (started != null) {
            sb.append(", started=").append(started);
        }
        if (finished != null) {
            sb.append(", finished=").append(finished);
        }
        sb.append(", state=").append(state);
        if (state == FINISHED) {
            sb.append(", result=").append(result);
        }
        if (reason != null) {
            sb.append(", reason=").append(reason);
        }
        if (taskId != null) {
            sb.append(", taskid=").append(taskId);
        }
        return sb.append("}").toString();
    }

}
