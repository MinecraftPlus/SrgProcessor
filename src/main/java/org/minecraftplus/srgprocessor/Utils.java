package org.minecraftplus.srgprocessor;

import org.minecraftplus.srgprocessor.tasks.deducer.Descriptor;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static final String PRIMITIVE_TYPES = "ZCBSIJFDV";
    public static final Pattern DESC = Pattern.compile("\\[*L(?<cls>[^;]+);|([" + PRIMITIVE_TYPES + "])");

    public static ArrayList<Descriptor> splitMethodDesc(String desc) {
        int beginIndex = desc.indexOf('(');
        int endIndex = desc.lastIndexOf(')');
        if((beginIndex == -1 && endIndex != -1) || (beginIndex != -1 && endIndex == -1)) {
            System.err.println(beginIndex);
            System.err.println(endIndex);
            throw new RuntimeException("Method description must start with '(' and end with ')'");
        }

        String x0;
        if(beginIndex == -1 && endIndex == -1) {
            x0 = desc;
        }
        else {
            x0 = desc.substring(beginIndex + 1, endIndex);
        }

        Matcher matcher = DESC.matcher(x0);
        ArrayList<Descriptor> matches = new ArrayList<>();
        while(matcher.find()) {
            matches.add(Descriptor.parse(matcher.group()));
        }

        return matches;
    }

    public static String[] splitCase(String input) {
        return input.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
    }
}
