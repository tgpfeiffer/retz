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
package io.github.retz.localexecutor;

import io.github.retz.scheduler.Resource;
import io.github.retz.scheduler.ResourceConstructor;
import io.github.retz.protocol.data.MetaJob;
import org.apache.mesos.Protos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class LocalProcess {
    private static final Logger LOG = LoggerFactory.getLogger(LocalProcess.class);
    long start;
    long end;
    private Process p;
    private Protos.TaskInfo task;
    private MetaJob metaJob;

    public LocalProcess(Protos.TaskInfo task, MetaJob metaJob) {
        this.task = Objects.requireNonNull(task);
        this.metaJob = metaJob;
    }

    public synchronized Protos.TaskInfo getTaskInfo() {
        return task;
    }

    public synchronized boolean start() {

        String path = ".";
        Resource resource = ResourceConstructor.decode(task.getResourcesList());

        try {
            // TODO: this is actually done in LocalProcessManager singleton thread, which may block other task invocations
            FileManager.fetchPersistentFiles(metaJob.getApp().getPersistentFiles(), path, metaJob.getJob().trustPVFiles());
        } catch (IOException e) {
            LOG.error("Cannot fetch persistent files: {}", e.toString());
            return false;
        }

        EnvBuilder envBuilder = new EnvBuilder((int)resource.cpu(), resource.memMB(), path);
        if (metaJob.getJob().props() != null) {
            envBuilder.putAll(metaJob.getJob().props());
        }
        String newHome = System.getProperty("user.dir", "/tmp");
        envBuilder.put("HOME", newHome); // move to temporary directory where Mesos opened tarball
        // Instead we adopt a kludge adding '-Duser.home=/tmp/....' to JVM arguments
        // envBuilder.put("YAESS_OPTS", "-Duser.home=" + newHome);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.environment().putAll(envBuilder.build());

        String[] localCmd = {"sh", "-c", metaJob.getJob().cmd()}; // TODO: do we really add another 'sh' layer.
        //cmd.addAll(Arrays.asList(metaJob.getJob().cmd().split("[\\s]+")));

        String sandbox = "/tmp";
        processBuilder.directory(new File(sandbox))
                .redirectError(new File(sandbox + "/stderr"))
                .redirectOutput(new File(sandbox + "/stdout"))
                .command(localCmd);

        start = System.currentTimeMillis();

        LOG.info("Running command: {}", metaJob.getJob().cmd()); //String.join(" ", argv));
        try {
            p = processBuilder.start();
        } catch (IOException e) {
            LOG.error(e.toString());
            return false;
        }
        if (Objects.isNull(p)) {
            LOG.error("failed to start process: {}", metaJob.getJob().cmd());
            return false;
        }
        return true;
    }

    public synchronized void kill() {
        p.destroy();
    }
    public synchronized boolean poll() throws InterruptedException {
        boolean ret = p.waitFor(0, TimeUnit.SECONDS);
        end = System.currentTimeMillis();
        if (ret)
            LOG.info("Command finished in {} seconds", (end - start) / 1000.0);
        return ret;
    }

    public int handle() {
        return p.exitValue();
    }
}
