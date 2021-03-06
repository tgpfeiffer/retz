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
package io.github.retz.admin;

import com.beust.jcommander.Parameter;
import com.j256.simplejmx.client.JmxClient;
import io.github.retz.cli.FileConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMException;
import javax.management.ObjectName;
import java.util.List;

public class CommandUsage implements SubCommand {
    static final Logger LOG = LoggerFactory.getLogger(CommandUsage.class);

    @Parameter(names = "-id", description = "Get usage of a user", required = true)
    private String id;

    @Parameter(names = "-start", description = "")
    private String start = "0";

    @Parameter(names = "-end", description = "")
    private String end = "9";

    @Override
    public String description() {
        return "Get usage of a user";
    }

    @Override
    public String getName() {
        return "usage";
    }

    @Override
    public int handle(FileConfiguration fileConfig) throws Throwable {
        try(AdminConsoleClient client = new AdminConsoleClient(new JmxClient("localhost", 9999))) {
            List<String> lines = client.getUsage(id, start, end);
            for(String line: lines) {
                LOG.info(line);
            }
            return 0;
        }
    }
}

