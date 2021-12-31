package org.minecraftplus.srgprocessor.tasks;

import java.io.IOException;
import net.minecraftforge.srgutils.CompleteRenamer;
import net.minecraftforge.srgutils.IMappingFile;

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

        switch (mode) {
        case RENAME: {
            log("Rename mode!");
            this.outputSrg = base.rename(new CompleteRenamer(source));
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
}
