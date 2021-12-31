package org.minecraftplus.srgprocessor.api;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraftforge.srgutils.IMappingFile.Format;
import org.minecraftplus.srgprocessor.tasks.SrgMapper;
import org.minecraftplus.srgprocessor.tasks.SrgWorker.Mode;

public class SrgMapperBuilder
{
    private PrintStream logStd = System.out;
    private PrintStream logErr = System.err;

    private List<Consumer<SrgMapper>> srgs = new ArrayList<>();
    private Mode mode = Mode.RENAME;
    private Format format = Format.TSRG2;
    private Path output = null;
    private boolean reverseOutput = false;

    public SrgMapperBuilder logger(PrintStream value) {
        this.logStd = value;
        return this;
    }

    public SrgMapperBuilder errorLogger(PrintStream value) {
        this.logErr = value;
        return this;
    }

    public SrgMapperBuilder srg(Path value) {
        this.srgs.add(a -> a.readSrg(value));
        return this;
    }

    public SrgMapperBuilder output(Path value) {
        if (value.getName(value.getNameCount()-1).toString().indexOf('.') > 0) {
            this.output = value;
        } else {
            this.output = value.resolveSibling(value.getName(value.getNameCount()-1) + "." + format.toString().toLowerCase());
        }
        return this;
    }

    public SrgMapperBuilder format(Format value) {
        this.format = value;
        return this;
    }

    public SrgMapperBuilder reverseOutput() {
        this.reverseOutput = true;
        return this;
    }

    public SrgMapperBuilder mode(Mode value) {
        this.mode = value;
        return this;
    }

    public SrgMapper build() {
        if (srgs.size() == 0)
            throw new IllegalArgumentException("Builder State Exception: Missing Srgs");
        if (output == null)
            throw new IllegalStateException("Builder State Exception: Missing Output");

        SrgMapper mapper = new SrgMapper().setLogger(logStd).setErrorLogger(logErr);

        mapper.mode(mode);
        mapper.output(output);
        mapper.outputFormat(format);
        mapper.reverseOutput(reverseOutput);

        srgs.forEach(e -> e.accept(mapper));

        return mapper;
    }
}
