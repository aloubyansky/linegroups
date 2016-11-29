/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.avoka.linegroups;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * @author olubyans
 *
 */
public class Main {

    public static void main(String[] args) throws Exception {

        // final LineGroup core = readGroup("standalone-core.xml");
        // final LineGroup servlet = readGroup("standalone-servlet.xml");
        // final LineGroup servletElytron = readGroup("standalone-servlet-elytron.xml");
        // final LineGroup servletLoadBalancer = readGroup("standalone-servlet-load-balancer.xml");
        final LineGroup standalone = readGroup("standalone.xml");
        final LineGroup standaloneFull = readGroup("standalone-full.xml");
        // final LineGroup standaloneHa = readGroup("standalone-ha.xml");
        // final LineGroup standaloneFullHa = readGroup("standalone-full-ha.xml");

        for (LineGroup g : Util.arrange(standaloneFull, standalone)) {
            System.out.println();
            System.out.println("GROUP " + g.getName());
            if (g.hasNestedGroups()) {
                final StringBuilder buf = new StringBuilder(" Includes: ");
                final String[] arr = g.getNestedGroupNames().toArray(new String[g.getNestedGroupNames().size()]);
                Arrays.sort(arr);
                buf.append(arr[0]);
                for (int i = 1; i < arr.length; ++i) {
                    buf.append(", ").append(arr[i]);
                }
                System.out.println(buf.toString());
            }
            if (g.size() > 0) {
                System.out.println(" Lines:");
                final String[] arr = g.getLines().toArray(new String[g.size()]);
                Arrays.sort(arr);
                for (String line : arr) {
                    System.out.println("  " + toCliLine(ModelNode.fromJSONString(line)));
                }
            }
        }
    }

    private static LineGroup readGroup(String name) throws IOException {
        final LineGroup.Builder builder = LineGroup.builder(name);
        try (BufferedReader reader = Files.newBufferedReader(getConfig(name))) {
            String line = reader.readLine();
            while (line != null) {
                builder.addLine(line);
                line = reader.readLine();
            }
        }
        return builder.build();
    }

    private static Path getConfig(String file) {
        final Path config = Paths.get("/home/olubyans/git/bootops/logged/standalone/json").resolve(file);
        if (!Files.exists(config)) {
            throw new IllegalStateException(config + " does not exist");
        }
        return config;
    }

    private static String toCliLine(ModelNode op) {
        final StringBuilder buf = new StringBuilder();
        buf.append('/');
        if (op.hasDefined("address")) {
            final List<Property> props = op.get("address").asPropertyList();
            if (!props.isEmpty()) {
                Property prop = props.get(0);
                buf.append(prop.getName()).append('=').append(prop.getValue().asString());
                if (props.size() > 1) {
                    for (int i = 1; i < props.size(); ++i) {
                        prop = props.get(i);
                        buf.append('/').append(prop.getName()).append('=').append(prop.getValue().asString());
                    }
                }
            }
        }
        buf.append(':').append(op.get("operation").asString());
        if (op.keys().size() > 2) {
            buf.append('(');
            int p = 0;
            for (String key : op.keys()) {
                if (key.equals("address") || key.equals("operation") || !op.hasDefined(key)) {
                    continue;
                }
                if (p++ > 0) {
                    buf.append(',');
                }
                buf.append(key).append("=");
                final ModelNode value = op.get(key);
                final boolean complexType = value.getType().equals(ModelType.OBJECT) || value.getType().equals(ModelType.LIST)
                        || value.getType().equals(ModelType.PROPERTY);
                final String strValue = value.asString();
                if (!complexType) {
                    buf.append("\"");
                    if (!strValue.isEmpty() && strValue.charAt(0) == '$') {
                        buf.append('\\');
                    }
                }
                buf.append(strValue);
                if (!complexType) {
                    buf.append('"');
                }
            }
            buf.append(')');
        }
        return buf.toString();
    }
}
