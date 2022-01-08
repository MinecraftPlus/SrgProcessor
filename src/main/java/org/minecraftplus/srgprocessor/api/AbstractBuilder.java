package org.minecraftplus.srgprocessor.api;

import net.minecraftforge.srgutils.IMappingFile;
import org.minecraftplus.srgprocessor.tasks.AbstractWorker;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractBuilder<T extends AbstractBuilder, U extends AbstractWorker> {

    PrintStream logStd = System.out;
    PrintStream logErr = System.err;

    protected boolean dryRun = false;
    protected List<Consumer<U>> inputs = new ArrayList<>();
    protected Path output = null;
    protected IMappingFile.Format format = IMappingFile.Format.TSRG2;
    protected boolean reverseOutput = false;

    public T logger(PrintStream value) {
        this.logStd = value;
        return (T) this;
    }

    public T errorLogger(PrintStream value) {
        this.logErr = value;
        return (T) this;
    }

    public T dryRun() {
        this.dryRun = true;
        return (T) this;
    }

    public T input(Path value) {
        this.inputs.add(a -> a.readSrg(value));
        return (T) this;
    }

    public T output(Path value) {
        if (value.getName(value.getNameCount()-1).toString().indexOf('.') > 0) {
            this.output = value;
        } else {
            this.output = value.resolveSibling(value.getName(value.getNameCount()-1) + "." + format.toString().toLowerCase());
        }
        return (T) this;
    }

    public T format(IMappingFile.Format value) {
        this.format = value;
        return (T) this;
    }

    public T reverseOutput() {
        this.reverseOutput = true;
        return (T) this;
    }

    public abstract U build();

    public final U postBuild(U worker) {
        if (inputs.size() == 0)
            throw new IllegalArgumentException("Builder State Exception: Missing input mappings");
        if (output == null)
            throw new IllegalStateException("Builder State Exception: Missing output destination");

        inputs.forEach(e -> e.accept(worker));

        worker.dryRun(dryRun);
        worker.output(output);
        worker.outputFormat(format);
        worker.reverseOutput(reverseOutput);

        worker.setLogger(logStd).setErrorLogger(logErr);

        return worker;
    }
}
