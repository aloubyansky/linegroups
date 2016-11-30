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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author Alexey Loubyansky
 */
public class Util {

    private static class LinesInGroups {

        final Map<String, Set<String>> lineInGroups = new HashMap<>();

        public void register(String line, LineGroup group) {
            Set<String> set = lineInGroups.get(line);
            if (set == null) {
                lineInGroups.put(line, Collections.singleton(group.getName()));
            } else {
                if (set.size() == 1) {
                    set = new HashSet<>(set);
                    lineInGroups.put(line, set);
                }
                set.add(group.getName());
            }
        }

        public void unregister(String line, String groupName) {
            Set<String> set = lineInGroups.get(line);
            if (!set.contains(groupName)) {
                return;
            }
            switch (set.size()) {
                case 1:
                    lineInGroups.remove(line);
                    break;
                case 2:
                    for (String str : set) {
                        if (!str.equals(groupName)) {
                            set = Collections.singleton(str);
                            break;
                        }
                    }
                    lineInGroups.put(line, set);
                    break;
                default:
                    set.remove(groupName);
            }
        }

        public Set<String> getGroupNames(String line) {
            return lineInGroups.get(line);
        }
    }

    public static Map<String, LineGroup> arrange(LineGroup... groups) {

        // order by line numbers
        Arrays.sort(groups, (o1, o2) -> o2.getLines().size() - o1.getLines().size());

        // check whether there are original groups that are fully included
        for(int i = 0; i < groups.length - 1; ++i) {
            LineGroup bigGroup = groups[i];
            for (int j = i + 1; j < groups.length; ++j) {
                LineGroup smallGroup = groups[j];
                if (bigGroup.getLines().containsAll(smallGroup.getLines())) {
                    final LineGroup.Builder bigBuilder = LineGroup.builder(bigGroup);
                    for(String extractedLine : smallGroup.getLines()) {
                        bigBuilder.removeLine(extractedLine);
                    }
                    bigBuilder.nestGroup(smallGroup.getName());
                    bigGroup = bigBuilder.build();
                    groups[i] = bigGroup;
                }
            }
        }

        final LinesInGroups linesInGroups = new LinesInGroups();
        final Map<String, LineGroup> lineGroups = new HashMap<>(groups.length);
        for (LineGroup group : groups) {
            if (lineGroups.put(group.getName(), group) != null) {
                throw new IllegalStateException("Duplicate group name " + group.getName());
            }
            for (String line : group.getLines()) {
                linesInGroups.register(line, group);
            }
        }

        Set<String> doneGroups = new HashSet<>();
        boolean extracted = true;
        while (extracted) {
            extracted = false;
            LineGroup.Builder lineGroupBuilder = null;
            Set<String> groupNames = null;
            final Set<String> processedLines = new HashSet<>();
            for (LineGroup group : lineGroups.values()) {
                if (doneGroups.contains(group.getName())) {
                    continue;
                }
                for (String line : group.getLines()) {
                    if (processedLines.contains(line)) {
                        continue;
                    }
                    processedLines.add(line);
                    groupNames = linesInGroups.getGroupNames(line);
                    if (groupNames.size() > 1) {
                        groupNames = new HashSet<>(groupNames);
                        for (String otherLine : group.getLines()) {
                            if (processedLines.contains(otherLine)) {
                                continue;
                            }
                            if (linesInGroups.getGroupNames(otherLine).containsAll(groupNames)) {
                                // form a group
                                if (lineGroupBuilder == null) {
                                    lineGroupBuilder = LineGroup.builder(UUID.randomUUID().toString()).addLine(line);
                                }
                                lineGroupBuilder.addLine(otherLine);
                            }
                        }
                        if (lineGroupBuilder != null) {
                            break;
                        }
                    }
                }

                if (lineGroupBuilder != null) {
                    break;
                } else {
                    doneGroups.add(group.getName());
                }
            }

            if (lineGroupBuilder != null) {
                LineGroup newGroup = lineGroupBuilder.build();

                final Map<String, LineGroup.Builder> rebuilders = new HashMap<>(groupNames.size());
                boolean originalNested = false;

                for (String groupName : groupNames) {
                    final LineGroup group = lineGroups.get(groupName);
                    if (group.getLines().size() == newGroup.getLines().size() && !group.hasNestedGroups()) {
                        if (originalNested) {
                            throw new IllegalStateException("Groups " + newGroup.getName() + " and " + group.getName()
                                    + " appear to be identical");
                        }
                        originalNested = true;
                        newGroup = group;
                        continue;
                    }
                    final LineGroup.Builder rebuilder = LineGroup.builder(group);
                    rebuilders.put(groupName, rebuilder);
                    for (String extractedLine : newGroup.getLines()) {
                        rebuilder.removeLine(extractedLine);
                        linesInGroups.unregister(extractedLine, groupName);
                    }
                }

                for (LineGroup.Builder rebuilder : rebuilders.values()) {
                    rebuilder.nestGroup(newGroup.getName());
                    final LineGroup group = rebuilder.build();
                    lineGroups.put(group.getName(), group);
                }

                if (!originalNested) {
                    lineGroups.put(newGroup.getName(), newGroup);
                    for (String line : newGroup.getLines()) {
                        linesInGroups.register(line, newGroup);
                    }
                    // doneGroups.add(newGroup.getName());
                }

                extracted = true;
            }
        }
        return lineGroups;
    }

    public static void main(String[] args) throws Exception {

        final LineGroup g1 = LineGroup.builder("g1").addLine("line1").addLine("line2").addLine("line3").addLine("line4")
                .build();
        final LineGroup g2 = LineGroup.builder("g2").addLine("line1").addLine("line2").build();
        final LineGroup g3 = LineGroup.builder("g3").addLine("line3").addLine("line4").addLine("line5").build();
        final LineGroup g4 = LineGroup.builder("g4").addLine("line1").addLine("line2").addLine("line3").addLine("line4")
                .addLine("line5").addLine("line6").build();

        final Collection<LineGroup> arranged = arrange(g1, g2, g3, g4).values();
        for (LineGroup g : arranged) {
            System.out.println(g);
            System.out.println();
        }
    }
}
