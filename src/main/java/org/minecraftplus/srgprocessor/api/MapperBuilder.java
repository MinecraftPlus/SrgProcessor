package org.minecraftplus.srgprocessor.api;

import org.minecraftplus.srgprocessor.tasks.Mapper;
import org.minecraftplus.srgprocessor.tasks.Mapper.Mode;

public class MapperBuilder extends Builder<MapperBuilder, Mapper> {

    private Mode mode = Mode.RENAME;

    public MapperBuilder mode(Mode value) {
        this.mode = value;
        return this;
    }

    @Override
    public Mapper build() {
        Mapper worker = new Mapper();
        worker.mode(mode);

        return this.postBuild(worker);
    }
}
