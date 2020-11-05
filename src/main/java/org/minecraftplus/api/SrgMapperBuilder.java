package org.minecraftplus.api;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.minecraftplus.map.SrgMapper;
import org.minecraftplus.util.io.FileOutputSupplier;
import net.minecraftforge.srg2source.api.OutputSupplier;

public class SrgMapperBuilder
{
    private PrintStream logStd = System.out;
    private PrintStream logErr = System.err;

    private List<Consumer<SrgMapper>> srgs = new ArrayList<>();
    private OutputSupplier output = null;
    
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
        try {
            this.output = new FileOutputSupplier(value);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid output: " + value, e);
        }
        return this;
    }

    public SrgMapper build() {
        if (srgs.size() == 0)
            throw new IllegalArgumentException("Builder State Exception: Missing Srgs");
        if (output == null)
            throw new IllegalStateException("Builder State Exception: Missing Output");

        SrgMapper mapper = new SrgMapper().setLogger(logStd).setErrorLogger(logErr);
        
        mapper.setOutput(output);

        srgs.forEach(e -> e.accept(mapper));

        return mapper;
    }
}
