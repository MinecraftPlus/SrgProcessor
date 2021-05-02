package org.minecraftplus.srgprocessor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraftforge.srg2source.util.io.ConfLogger;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IMappingFile.Format;
import net.minecraftforge.srgutils.IMappingFile.IClass;
import net.minecraftforge.srgutils.IMappingFile.IField;
import net.minecraftforge.srgutils.IMappingFile.IMethod;
import net.minecraftforge.srgutils.IMappingFile.IParameter;

public abstract class SrgWorker<T extends ConfLogger<T>> extends ConfLogger<T>
{

    public final static BiPredicate<IClass, IClass> MATCHING_CLASS = (m1,
        m2) -> (m1 != null && m2 != null && m1.getOriginal().equals(m2.getOriginal()));
    public final static BiPredicate<IMethod, IMethod> MATCHING_METHOD = (m1, m2) -> (m1 != null && m2 != null
        && m1.getOriginal().equals(m2.getOriginal()) && m1.getDescriptor().equals(m2.getDescriptor()));
    public final static BiPredicate<IField, IField> MATCHING_FIELD = (f1,
        f2) -> (f1 != null && f2 != null && f1.getOriginal().equals(f2.getOriginal()));
    public final static BiPredicate<IParameter, Integer> MATCHING_PARAM = (p,
        i) -> (p != null && i != null && p.getIndex() == i);

    List<IMappingFile> srgs = new ArrayList<>();
    Mode mode;
    IMappingFile outputSrg;
    Path outputPath;
    Format outputFormat;
    boolean reverseOutput = false;

    public void readSrg(Path srg) {
        try (InputStream in = Files.newInputStream(srg)) {
            IMappingFile map = IMappingFile.load(in);
            srgs.add(map);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read SRG: " + srg, e);
        }
    }

    public void mode(Mode value) {
        this.mode = value;
    }

    public void output(Path output) {
        this.outputPath = output;
    }

    public void outputFormat(Format value) {
        this.outputFormat = value;
    }

    public void reverseOutput(boolean value) {
        this.reverseOutput = value;
    }

    public abstract void run() throws IOException;

    public void write() throws IOException {
        this.outputSrg.write(outputPath, outputFormat, reverseOutput);
    }

    public enum Mode
    {
        RENAME("Generate mapping from base original to source mapped"),
        CHAIN("Generate mapping from base mapped to source mapped"),
        FILL("Generate mapping of base filled with missing source entries");

        String desc;

        Mode(String desc) {
            this.desc = desc;
        }
    }
}
