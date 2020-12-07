package net.minecraftforge.srgutils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.BiPredicate;
import net.minecraftforge.srg2source.util.io.ConfLogger;
import net.minecraftforge.srgutils.IMappingFile.Format;
import net.minecraftforge.srgutils.IMappingFile.IField;
import net.minecraftforge.srgutils.IMappingFile.IMethod;
import net.minecraftforge.srgutils.MappingFile.Cls;
import net.minecraftforge.srgutils.MappingFile.Cls.Field;
import net.minecraftforge.srgutils.MappingFile.Cls.Method;

public class SrgMapper extends ConfLogger<SrgMapper>
{
    public final static BiPredicate<IMethod, IMethod> MATCHING_METHOD = (m1, m2) -> (m1 != null && m2 != null
        && m1.getOriginal().equals(m2.getOriginal()) && m1.getDescriptor().equals(m2.getDescriptor()));
    public final static BiPredicate<IField, IField> MATCHING_FIELD = (f1,
        f2) -> (f1 != null && f2 != null && f1.getOriginal().equals(f2.getOriginal()));

    private List<IMappingFile> srgs = new ArrayList<>();

    private Path outputPath = null;
    private MappingFile outputSrg;

    private boolean fillMissing = true;

    public void readSrg(Path srg) {
        try (InputStream in = Files.newInputStream(srg)) {
            IMappingFile map = IMappingFile.load(in);
            srgs.add(map);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read SRG: " + srg, e);
        }
    }

    public void setOutput(Path output) {
        this.outputPath = output;
    }

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

        this.outputSrg = new MappingFile();

        MappingFile base = (MappingFile)srgs.get(0);
        // base.reverse();

        for (int n = 1; n < srgs.size(); n++) {
            MappingFile source = (MappingFile)srgs.get(n);

            for (Cls sourceClass : source.getClasses()) {

                Cls baseClass = base.getClass(sourceClass.getOriginal());
                if (baseClass == null) { // Try to recover class if missing
                    String original = sourceClass.getOriginal();
                    String remapped = base.remapClass(original);

                    // Recover only if name differs or fillMising flag is true
                    if (!original.equals(remapped) || this.fillMissing) {
                        baseClass = base.addClass(original, remapped);
                        log(" Recovered base class from: '" + original + " : " + remapped + "'");
                        log("  result: " + baseClass);
                    } else { // Otherwise abort
                        log("Aborted class recovering:");
                        log(" sourceClass: " + sourceClass);
                        log("    original: " + original);
                        log("    remapped: " + remapped);
                        continue;
                    }
                }

                // Add class to out mapping
                Cls processedClass = this.outputSrg.addClass(baseClass.getMapped(),  sourceClass.getMapped());
                log("Mapped classes:");
                log(" from: " + baseClass);
                log("   to: " + sourceClass);

                for (Method sourceMethod : sourceClass.getMethods()) {

                    if (baseClass != null) {

                        Method baseMethod = null;

                        // Find field in base mapping
                        Optional<? extends Method> search = baseClass.getMethods().stream()
                            .filter(m -> MATCHING_METHOD.test(m, sourceMethod)).findFirst();
                        if (search.isPresent()) {
                            baseMethod = search.get();
                        }

                        if (baseMethod != null) {
                            String original = baseMethod.getMapped();
                            String desc = baseMethod.getDescriptor();
                            String mappedDesc = baseMethod.getMappedDescriptor();
                            String mapped = sourceMethod.getMapped();

                            if (mappedDesc == null || mappedDesc == sourceMethod.getDescriptor()) {
                                throw new IllegalStateException("Null desc!");
                            }

                            processedClass.addMethod(original, mappedDesc, mapped);
                            log("Mapped method " + baseMethod + " to " + sourceMethod);
                            continue;
                        }
                    }

                    // Put method mapping from source
                    processedClass.addMethod(sourceMethod.getOriginal(),
                        base.remapDescriptor(sourceMethod.getDescriptor()), sourceMethod.getMapped());
                }

                for (Field sourceField : sourceClass.getFields()) {

                    if (baseClass != null) {

                        Field baseField = null;

                        // Find field in base mapping
                        Optional<? extends Field> s = baseClass.getFields().stream()
                            .filter(f -> MATCHING_FIELD.test(f, sourceField)).findFirst();
                        if (s.isPresent()) {
                            baseField = s.get();
                        }

                        if (baseField != null) {
                            processedClass.addField(baseField.getMapped(), sourceField.getMapped(),
                                baseField.getMappedDescriptor());
                            log("Mapped field " + baseField + " to " + sourceField);
                            continue;
                        }
                    }

                    // Put field mapping from source
                    processedClass.addField(sourceField.getOriginal(), sourceField.getMapped(),
                        sourceField.getDescriptor());
                }
            }
        }

        this.outputSrg.write(this.outputPath, Format.TSRG, false);

        log("Srg mapping done!");
    }
}
