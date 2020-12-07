package org.minecraftplus.api;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraftforge.srgutils.SrgMapper;

public class SrgMapperBuilder
{
    private PrintStream logStd = System.out;
    private PrintStream logErr = System.err;

    private List<Consumer<SrgMapper>> srgs = new ArrayList<>();
    private Path output = null;
    private boolean fillMissing = false;
    private boolean filterSameNames = false;

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
        this.output = value;
        return this;
    }

    public SrgMapperBuilder fillMissing() {
        this.fillMissing = true;
        return this;
    }

    public SrgMapperBuilder filterSameNames() {
        this.filterSameNames = true;
        return this;
    }

    public SrgMapper build() {
        if (srgs.size() == 0)
            throw new IllegalArgumentException("Builder State Exception: Missing Srgs");
        if (output == null)
            throw new IllegalStateException("Builder State Exception: Missing Output");

        SrgMapper mapper = new SrgMapper().setLogger(logStd).setErrorLogger(logErr);

        mapper.setOutput(output);
        mapper.fillMissing(fillMissing);
        mapper.filterSameNames(filterSameNames);

        srgs.forEach(e -> e.accept(mapper));

        return mapper;
    }
}
