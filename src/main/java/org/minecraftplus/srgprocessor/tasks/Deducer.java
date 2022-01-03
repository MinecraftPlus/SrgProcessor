package org.minecraftplus.srgprocessor.tasks;

import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IRenamer;
import org.minecraftplus.srgprocessor.tasks.deducer.Dictionary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Deducer extends SrgWorker<Deducer>
{
    List<Dictionary> dicts = new ArrayList<>();

    public void readDictionary(Path value) {
        try (InputStream in = Files.newInputStream(value)) {
            Dictionary dict = new Dictionary().load(in);
            dicts.add(dict);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read dictionary file: " + value, e);
        }
    }

    @Override
    public void run() throws IOException {
        if (srgs == null)
            throw new IllegalStateException("Missing Srg srgs");
        if (srgs.size() < 1)
            throw new IllegalStateException("Required 1 srgs to process");
        if (outputPath == null)
            throw new IllegalStateException("Missing Srg output");

        Nlog("Informations:");
        for (IMappingFile file : srgs) {
            log(" class count: " + file.getClasses().size());
        }

        Nlog("Processing...");
        IMappingFile input = srgs.get(0);
        this.outputSrg = input.rename(new IRenamer() {
            @Override
            public String rename(IMappingFile.IParameter value) {
                String ret = value.getMapped();
                for (Dictionary dictionary : dicts) {
                    ret = dictionary.deduceName(value);
                }
                return ret;
            }

            @Override
            public String rename(IMappingFile.IField value) { //FOR DEBUG
                return value.getOriginal();
            }
        });

        this.outputSrg = this.outputSrg.filter(); //FOR DEBUG

        Nlog("Writing to output...");
        write();

        logN("Deducing done!");
    }
}
