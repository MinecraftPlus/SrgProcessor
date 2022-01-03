package org.minecraftplus.srgprocessor.tasks.deducer;

import net.minecraftforge.srgutils.IMappingFile;
import org.minecraftplus.srgprocessor.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Dictionary {

    private final Map<Pattern, String> rules = new LinkedHashMap<>();

    public Dictionary load(InputStream in) throws IOException {
        List<String> lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                .map(Dictionary::stripComment)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());

        for (String line : lines) {
            String[] pts = line.split(" ");
            if (pts.length > 2)
                throw new IOException("Invalid dictionary line, too many parts: " + line);

            rules.put(Pattern.compile(pts[0]), pts[1]);
        }

        return this;
    }

    public String deduceName(IMappingFile.IParameter parameter) {
        IMappingFile.IMethod method = parameter.getParent();
        String descriptor = method.getMappedDescriptor();

        List<Descriptor> descriptors = Utils.splitMethodDesc(descriptor);
        Descriptor parameterDescriptor = descriptors.get(parameter.getIndex());

        String parameterName = parameterDescriptor.name;
        parameterName = parameterName.substring(parameterName.lastIndexOf("/") + 1);
        parameterName = parameterName.substring(parameterName.lastIndexOf("$") + 1);

        String[] parameterNameWords = Utils.splitCase(parameterName);

        // Add 'a' prefix to parameters which are arrays
        if (parameterDescriptor.array)
            parameterName = "a" + parameterName;

        for (Map.Entry<Pattern, String> rule : this.rules.entrySet()) {
            Matcher matcher = rule.getKey().matcher(parameterName);
            while (matcher.find()) {
                parameterName = matcher.replaceFirst(rule.getValue());
            }
        }

        return parameterName.toLowerCase(Locale.ROOT);
    }

    private static String stripComment(String str) {
        int idx = str.indexOf('#');
        if (idx == 0)
            return "";
        if (idx != -1)
            str = str.substring(0, idx - 1);
        int end = str.length();
        while (end > 1 && str.charAt(end - 1) == ' ')
            end--;
        return end == 0 ? "" : str.substring(0, end);
    }
}
