package org.minecraftplus.srgprocessor.tasks.deducer;

import org.minecraftplus.srgprocessor.Utils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Action {

    Type type = Type.RENAME;
    String value;
    Pattern filter;

    public Action(String line) throws IOException {
        String[] pts = line.split(":");
        if (pts.length < 1)
            throw new IOException("Invalid action line, not enough parts: " + line);

        try {
            this.type = Type.valueOf(pts[0]);
        } catch (IllegalArgumentException e) {
            // If regular string in dictionary, it mean replace
            this.value = line;
            return;
        }

        // Add package filter if specified
        if (pts.length >= 3)
            this.filter = Pattern.compile(pts[2]);

        switch (type) {
            case RENAME:
            case PREFIX:
            case SUFFIX:
                if (pts.length < 2)
                    throw new IOException("Invalid action line, no value for action: " + line);
                this.value = pts[1];
                break;
            case FIRST:
            case LAST:
                break;
            default:
                throw new IllegalStateException("Wait, that's illegal.");
        }
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public Pattern getFilter() {
        return filter;
    }

    public String act(Matcher matcher, Descriptor descriptor) {

        // Check package filter, if not match return untouched
        if (filter != null) {
            if (!filter.matcher(descriptor.getName()).matches())
                return matcher.group();
        }

        // Do action, return refactored
        switch (type) {
            case RENAME:
                return matcher.replaceFirst(this.value);
            case PREFIX:
                return this.value + matcher.group();
            case SUFFIX:
                return matcher.group() +  this.value;
            case FIRST:
                return Utils.splitCase(matcher.group())[0];
            case LAST:
                String[] words = Utils.splitCase(matcher.group());
                return words[words.length - 1];
            default:
                throw new IllegalStateException("Wait, that's illegal.");
        }
    }

    enum Type {
        RENAME, PREFIX, SUFFIX, FIRST, LAST;
    }

    enum Operator {
        NOT, AND, OR;
    }
}
