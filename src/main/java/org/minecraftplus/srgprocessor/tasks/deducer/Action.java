package org.minecraftplus.srgprocessor.tasks.deducer;

import java.io.IOException;
import java.util.regex.Matcher;

public class Action {

    Type type = Type.RENAME;
    String value;

    public Action(String line) throws IOException {
        System.out.println("Line: " + line);

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

        switch (type) {
            case RENAME:
            case PREFIX:
            case SUFFIX:
                if (pts.length < 2)
                    throw new IOException("Invalid action line, no value for action: " + line);

                this.value = pts[1];
                break;
            default:
                throw new IllegalStateException("Wait, that's illegal.");
        }
    }

    public String act(Matcher matcher) {
        switch (type) {
            case RENAME:
                return matcher.replaceFirst(this.value);
            case PREFIX:
                return this.value + matcher.group();
            case SUFFIX:
                return matcher.group() +  this.value;
            default:
                throw new IllegalStateException("Wait, that's illegal.");
        }
    }

    enum Type {
        RENAME, PREFIX, SUFFIX;
    }

    enum Operator {
        NOT, AND, OR;
    }
}
