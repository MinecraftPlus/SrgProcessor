package org.minecraftplus.srgprocessor.tasks.deducer;

import org.minecraftplus.srgprocessor.Utils;

import java.util.regex.Matcher;

public class Descriptor {

    boolean primitive;
    boolean array;
    String name;

    public Descriptor(boolean primitive, boolean isArray, String className) {
        this.primitive = primitive;
        this.array = isArray;
        this.name = className;
    }

    public static Descriptor parse(String desc) {
        boolean primitive = (Utils.PRIMITIVE_TYPES.contains(desc)
                && (desc.charAt(0) != 'L' && desc.charAt(desc.length() - 1) != ';'));

        boolean isArray = desc.lastIndexOf('[') != -1;

        Matcher matcher = Utils.DESC.matcher(desc);
        if (matcher.find()) {
            String className = matcher.group("cls");
            if (className == null)
                className = matcher.group();

            return new Descriptor(primitive, isArray, className);
        } else
            throw new IllegalStateException("Description cannot not have any class!");
    }

    @Override
    public String toString() {
        return "Descriptor{" +
                "primitive=" + primitive +
                ", array=" + array +
                ", name='" + name + '\'' +
                '}';
    }
}
