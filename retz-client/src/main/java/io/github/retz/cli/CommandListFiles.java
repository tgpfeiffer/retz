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
package io.github.retz.cli;

import com.beust.jcommander.Parameter;
import io.github.retz.protocol.ErrorResponse;
import io.github.retz.protocol.ListFilesRequest;
import io.github.retz.protocol.ListFilesResponse;
import io.github.retz.protocol.Response;
import io.github.retz.protocol.data.DirEntry;
import io.github.retz.web.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommandListFiles implements SubCommand {
    static final Logger LOG = LoggerFactory.getLogger(CommandListFiles.class);

    @Parameter(names = "-id", description = "Job ID whose state and details you want", required = true)
    private int id;

    @Parameter(names = "--path", description = "Remote sandbox path to list")
    private String path = ListFilesRequest.DEFAULT_SANDBOX_PATH; // This cannot be empty string or '.' as SparkJava's router doesn't route them

    @Override
    public String description() {
        return "List files in the sandbox of a job";
    }

    @Override
    public String getName() {
        return "list-files";
    }

    @Override
    public int handle(ClientCLIConfig fileConfig) {
        LOG.debug("Configuration: {}", fileConfig.toString());

        try (Client webClient = Client.newBuilder(fileConfig.getUri())
                .enableAuthentication(fileConfig.authenticationEnabled())
                .setAuthenticator(fileConfig.getAuthenticator())
                .checkCert(fileConfig.checkCert())
                .build()) {

            LOG.info("Listing files in {} of a job(id={})", path, id);

            Response res = webClient.listFiles(id, path);
            if (res instanceof ListFilesResponse) {
                ListFilesResponse listFilesResponse = (ListFilesResponse) res;

                if (listFilesResponse.job().isPresent()) {
                    //Job job = getJobResponse.job().get();

                    //LOG.info("Job: appid={}, id={}, scheduled={}, cmd='{}'", job.appid(), job.id(), job.scheduled(), job.cmd());
                    //LOG.info("\tstarted={}, finished={}, state={}, result={}", job.started(), job.finished(), job.state(), job.result());
                    TableFormatter formatter = new TableFormatter("gid", "mode", "uid", "mtime", "size", "path");

                    DateFormat df = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
                    for (DirEntry e : listFilesResponse.entries()) {
                        String filename = e.path();
                        Path file = Paths.get(filename).getFileName();
                        if (file != null) {
                            //Path#getFileName() returns null when it's ...
                            filename = file.toString();
                        }
                        Date date = new Date(e.mtime() * 1000);
                        formatter.feed(e.gid(), e.mode(), e.uid(), df.format(date), Integer.toString(e.size()), filename);
                        //LOG.info(e.toString());
                    }
                    LOG.info(formatter.titles());
                    for (String line : formatter) {
                        LOG.info(line);
                    }

                    return 0;

                } else {
                    LOG.error("No such job: id={}", id);
                }
            } else {
                ErrorResponse errorResponse = (ErrorResponse) res;
                LOG.error("Error: {}", errorResponse.status());
            }

        } catch (ConnectException e) {
            LOG.error("Cannot connect to server {}", fileConfig.getUri());
        } catch (IOException e) {
            LOG.error(e.toString(), e);
        }
        return -1;

    }
}

