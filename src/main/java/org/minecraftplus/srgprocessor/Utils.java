package org.minecraftplus.srgprocessor;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static final Pattern DESC = Pattern.compile("L(?<cls>[^;]+);|([ZBCSIFDJ])");

    public static ArrayList<String> splitMethodDesc(String desc) {
        int beginIndex = desc.indexOf('(');
        int endIndex = desc.lastIndexOf(')');
        if((beginIndex == -1 && endIndex != -1) || (beginIndex != -1 && endIndex == -1)) {
            System.err.println(beginIndex);
            System.err.println(endIndex);
            throw new RuntimeException();
        }
        String x0;
        if(beginIndex == -1 && endIndex == -1) {
            x0 = desc;
        }
        else {
            x0 = desc.substring(beginIndex + 1, endIndex);
        }

        Matcher matcher = DESC.matcher(x0);
        ArrayList<String> listMatches = new ArrayList<>();
        while(matcher.find()) {
            String cls = matcher.group("cls");
            listMatches.add(cls != null ? cls : matcher.group());
        }
        return listMatches;
    }
}
