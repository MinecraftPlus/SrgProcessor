package org.minecraftplus.srgprocessor.tasks;

import net.minecraftforge.srgutils.IMappingBuilder;
import net.minecraftforge.srgutils.IMappingFile;
import org.minecraftplus.srgprocessor.Utils;
import org.minecraftplus.srgprocessor.tasks.deducer.Descriptor;

import java.io.IOException;
import java.util.ArrayList;

public class Cleaner extends AbstractWorker<Cleaner>
{

    private boolean inferParameters = false;
    private boolean filterSameNames = false;

    public void inferParameters(boolean infer) { this.inferParameters = infer; }

    public void filterSameNames(boolean filter) { this.filterSameNames = filter; }

    @Override
    public void run() throws IOException {
        if (srgs == null)
            throw new IllegalStateException("Missing input SRG's");
        if (srgs.size() < 1)
            throw new IllegalStateException("Required one input SRG process");
        if (outputPath == null)
            throw new IllegalStateException("Missing output SRG destination");

        if (filterSameNames && inferParameters) {
            System.out.println("# Infering parameters and filtering at the same moment can limit class and");
            System.out.println("# methods filtering capabilities due to generated parameters are not equal!");
        }

        log("Process informations:");
        log(" " + srgs.size() + " input srg(s)");
        for (IMappingFile file : srgs) {
            log("  srg (" + file.getClasses().size() + " classes)");
        }

        log("Cleaning input mapping...");
        this.outputSrg = srgs.get(0);

        if (inferParameters) {
            log("Infering method parameters...");
            IMappingBuilder builder = IMappingBuilder.create();

            this.outputSrg.getClasses().stream().forEach(cls -> {
                IMappingBuilder.IClass builderCls = builder.addClass(cls.getOriginal(), cls.getMapped());
                cls.getMetadata().entrySet().stream().forEach(m -> builderCls.meta(m.getKey(), m.getValue()));
                cls.getFields().stream().forEach(fld -> {
                    IMappingBuilder.IField builderField = builderCls.field(fld.getOriginal(), fld.getMapped())
                            .descriptor(fld.getDescriptor()); // TODO Maybe can do add descriptor in .field(o, m) method?
                    fld.getMetadata().entrySet().stream().forEach(m -> builderField.meta(m.getKey(), m.getValue()));
                });
                cls.getMethods().stream().forEach(mtd -> {
                    IMappingBuilder.IMethod builderMtd = builderCls.method(mtd.getDescriptor(), mtd.getOriginal(), mtd.getMapped());
                    mtd.getMetadata().entrySet().stream().forEach(m -> builderMtd.meta(m.getKey(), m.getValue()));

                    // Get method parameter list from descriptor
                    ArrayList<Descriptor> parameters = Utils.splitMethodDesc(mtd.getDescriptor());
                    for (int parNumber = 0; parNumber < parameters.size(); parNumber++) {
                        IMappingFile.IParameter parameter = mtd.getParameter(parNumber);
                        if (parameter == null) {
                            String parent = mtd.getMapped();

                            if (parent.equals("<init>")) { // When mapping constructor use class name
                                String a = mtd.getParent().getMapped();
                                a = a.substring(a.lastIndexOf("/") + 1);
                                a = a.substring(a.lastIndexOf("$") + 1);
                                parent = parent.replaceAll("<init>", a);
                            }

                            // Combine new parameter name
                            String inferredName = "p_" + parent + "_" + (parNumber + 1);
                            builderMtd.parameter(parNumber, "inferred", inferredName);
                        }
                    }
                });
            });

            // Get generated mapping from builder
            this.outputSrg = builder.build().getMap("left", "right");
        }

        if (filterSameNames) {
            log("Filtering output...");
            this.outputSrg = this.outputSrg.filter();
        }

        log("Writing to output...");
        write();

        Nlog("Clean task done!");
    }
}
