package org.minecraftplus.srgprocessor.api;

import net.minecraftforge.srgutils.IMappingFile.Format;
import org.minecraftplus.srgprocessor.tasks.Deducer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DeducerBuilder extends Builder<DeducerBuilder, Deducer> {

    private List<Consumer<Deducer>> dicts = new ArrayList<>();

    public DeducerBuilder dictionary(Path value) {
        this.dicts.add(a -> a.readDictionary(value));
        return this;
    }

    @Override
    public Deducer build() {
        if (dicts.size() == 0)
            throw new IllegalArgumentException("Builder State Exception: Missing dictionaries");

        Deducer worker = new Deducer();
        dicts.forEach(e -> e.accept(worker));

        return this.postBuild(worker);
    }
}
