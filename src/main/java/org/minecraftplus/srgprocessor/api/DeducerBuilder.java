package org.minecraftplus.srgprocessor.api;

import org.minecraftplus.srgprocessor.tasks.Deducer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DeducerBuilder extends AbstractBuilder<DeducerBuilder, Deducer> {

    private List<Consumer<Deducer>> dicts = new ArrayList<>();
    private boolean collectStats = false;

    public DeducerBuilder dictionary(Path value) {
        this.dicts.add(a -> a.readDictionary(value));
        return this;
    }

    public DeducerBuilder collectStatistics() {
        this.collectStats = true;
        return this;
    }

    @Override
    public Deducer build() {
        Deducer worker = new Deducer();
        worker.collectStatistics(collectStats);
        dicts.forEach(e -> e.accept(worker));

        return this.postBuild(worker);
    }
}
