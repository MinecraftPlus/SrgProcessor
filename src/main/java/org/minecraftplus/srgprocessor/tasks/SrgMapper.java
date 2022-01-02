package org.minecraftplus.srgprocessor.tasks;

import java.io.IOException;
import net.minecraftforge.srgutils.CompleteRenamer;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IMappingFile.IClass;
import net.minecraftforge.srgutils.IRenamer;

public class SrgMapper extends SrgWorker<SrgMapper>
{
    @Override
    public void run() throws IOException {
        if (srgs == null)
            throw new IllegalStateException("Missing Srg Mapper srgs");
        if (srgs.size() < 2)
            throw new IllegalStateException("Required at least 2 srgs");
        if (outputPath == null)
            throw new IllegalStateException("Missing Srg Mapper output");

        log("\nBuild informations:");
        for (IMappingFile file : srgs) {
            log(" Maping size: " + file.getClasses().size());
        }
        log("\n");

        IMappingFile base = srgs.get(0);
        IMappingFile source = srgs.get(1);

        switch ((Mode)mode) {
        case RENAME: {
            log("Rename ALL mode!");
            this.outputSrg = base.rename(new CompleteRenamer(source));
            break;
        }
        case RENAME_CLASSES: {
            log("Rename CLASSES mode!");
            this.outputSrg = base.rename(new IRenamer() {
                @Override
                public String rename(IClass value) {
                    IClass cls = source.getClass(value.getOriginal());
                    return cls == null ? value.getMapped() : cls.getMapped();
                }
            });
            break;
        }
        case CHAIN: {
            log("Chain mode!");
            this.outputSrg = base.reverse().chain(source);
            break;
        }
        case FILL: {
            log("Fill mode!");
            this.outputSrg = base.fill(source);
            break;
        }
        default:
            log("Unknown mode!");
            break;
        }

        log("Writing SRG...");
        write();

        log("Srg mapping done!\n");
    }

    public enum Mode implements SrgWorker.Mode
    {
        RENAME("Generate mapping from base original to source mapped"),
        RENAME_CLASSES("Generate mapping from base original to source mapped only for classes"),
        CHAIN("Generate mapping from base mapped to source mapped"),
        FILL("Generate mapping of base filled with missing source entries"),
        GENERATE_PARAM("Generate method parameters names from parameter type");

        String desc;

        Mode(String desc) {
            this.desc = desc;
        }
    }
}
