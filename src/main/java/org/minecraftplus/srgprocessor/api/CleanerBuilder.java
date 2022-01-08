package org.minecraftplus.srgprocessor.api;

import org.minecraftplus.srgprocessor.tasks.Cleaner;

public class CleanerBuilder extends AbstractBuilder<CleanerBuilder, Cleaner> {

    private boolean inferParameters = false;
    private boolean filterSameNames = false;

    @Override
    public Cleaner build() {
        Cleaner worker = new Cleaner();
        worker.inferParameters(inferParameters);
        worker.filterSameNames(filterSameNames);

        return this.postBuild(worker);
    }

    public CleanerBuilder infer(boolean inferParameters) {
        this.inferParameters = inferParameters;
        return this;
    }

    public CleanerBuilder filter(boolean filterSameNames) {
        this.filterSameNames = filterSameNames;
        return this;
    }
}
