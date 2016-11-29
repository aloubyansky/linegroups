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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Alexey Loubyansky
 */
public class LineGroup {

    public static class Builder {

        private String name;
        private Set<String> lines = Collections.emptySet();
        private Map<String, LineGroup> nestedGroups = Collections.emptyMap();

        private Builder(String name) {
            this.name = name;
        }

        private Builder(LineGroup group) {
            this.name = group.name;
            if (!group.lines.isEmpty()) {
                if (group.lines.size() > 1) {
                    lines = new HashSet<String>(group.lines);
                } else {
                    lines = Collections.singleton(group.lines.iterator().next());
                }
            }
            if (group.hasNestedGroups()) {
                if (group.nestedGroups.size() == 1) {
                    final Entry<String, LineGroup> entry = group.nestedGroups.entrySet().iterator().next();
                    nestedGroups = Collections.singletonMap(entry.getKey(), entry.getValue());
                } else {
                    nestedGroups = new HashMap<>(group.nestedGroups);
                }
            }
        }

        public Builder addLine(String line) {
            switch (lines.size()) {
                case 0:
                    lines = Collections.singleton(line);
                    break;
                case 1:
                    lines = new HashSet<>(lines);
                default:
                    lines.add(line);
            }
            return this;
        }

        public Builder removeLine(String line) {
            if (lines.contains(line)) {
                switch (lines.size()) {
                    case 1:
                        lines = Collections.emptySet();
                        break;
                    case 2:
                        for (String str : lines) {
                            if (!str.equals(line)) {
                                lines = Collections.singleton(str);
                            }
                        }
                        break;
                    default:
                        lines.remove(line);
                }
            }
            return this;
        }

        public int linesTotal() {
            return lines.size();
        }

        public Builder nestGroup(LineGroup nested) {
            switch (nestedGroups.size()) {
                case 0:
                    nestedGroups = Collections.singletonMap(nested.getName(), nested);
                    break;
                case 1:
                    nestedGroups = new HashMap<>(nestedGroups);
                default:
                    nestedGroups.put(nested.getName(), nested);
            }
            return this;
        }

        public LineGroup build() {
            return new LineGroup(this);
        }
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static Builder builder(LineGroup group) {
        return new Builder(group);
    }

    private final String name;
    private final Map<String, LineGroup> nestedGroups;
    private final Set<String> lines;

    private LineGroup(Builder builder) {
        this.name = builder.name;
        this.lines = Collections.unmodifiableSet(builder.lines);
        this.nestedGroups = Collections.unmodifiableMap(builder.nestedGroups);
    }

    public String getName() {
        return name;
    }

    public int size() {
        return lines.size();
    }

    public Set<String> getLines() {
        return lines;
    }

    public boolean hasNestedGroups() {
        return !nestedGroups.isEmpty();
    }

    public Set<String> getNestedGroupNames() {
        return nestedGroups.keySet();
    }

    public LineGroup getNestedGroup(String name) {
        return nestedGroups.get(name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((lines == null) ? 0 : lines.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nestedGroups == null) ? 0 : nestedGroups.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LineGroup other = (LineGroup) obj;
        if (lines == null) {
            if (other.lines != null)
                return false;
        } else if (!lines.equals(other.lines))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (nestedGroups == null) {
            if (other.nestedGroups != null)
                return false;
        } else if (!nestedGroups.equals(other.nestedGroups))
            return false;
        return true;
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("[").append(name).append(":");
        for (String line : lines) {
            buf.append("\n").append(line);
        }
        if (!nestedGroups.isEmpty()) {
            final String[] arr = nestedGroups.keySet().toArray(new String[nestedGroups.size()]);
            Arrays.sort(arr);
            buf.append("\n").append("groups=").append(Arrays.asList(arr));
        }
        return buf.append(']').toString();
    }
}
