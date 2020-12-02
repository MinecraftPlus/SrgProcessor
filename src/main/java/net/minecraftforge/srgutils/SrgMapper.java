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
    private List<IMappingFile> srgs = new ArrayList<>();
//    private Map<Integer, Map<String, String>> clsSrc2Internal = new HashMap<Integer, Map<String, String>>();

    private Path output = null;

    MappingFile outputSrg;

    public void readSrg(Path srg) {
        try (InputStream in = Files.newInputStream(srg)) {
            IMappingFile map = IMappingFile.load(in);
            srgs.add(map);

//            Map<String, String> internals = new HashMap<>();
//            map.getClasses()
//                .forEach(c -> internals.put(c.getOriginal().replace('/', '.').replace('$', '.'), c.getMapped()));
//            clsSrc2Internal.put(map.hashCode(), internals);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read SRG: " + srg, e);
        }
    }

    public void setOutput(Path output) {
        this.output = output;
    }

    static Scanner consoleScanner = new Scanner(System.in);

    //Jobs  
    static Progress classMappingProgress = new Progress();
    static Progress fieldMappingProgress = new Progress();
    static Progress methodMappingProgress = new Progress();
    static Progress unusedClassesMappingProgress = new Progress();
    static Progress unusedFieldMappingProgress = new Progress();
    static Progress unusedMethodMappingProgress = new Progress();

    static String[] ignoredPackages = new String[] { "java/", "javax/", "com/", "org/", "it/", "io/",
        "net/minecraft/realms", "joptsimple/" };

    static boolean isIgnored(String className) {
        for (String ignore : ignoredPackages) {
            if (className.startsWith(ignore))
                return true;
        }
        return false;
    }

    String TESTCLASS = "cca";

    public void run() throws IOException {
        if (srgs == null)
            throw new IllegalStateException("Missing Srg Mapper srgs");
        if (output == null)
            throw new IllegalStateException("Missing Srg Mapper output");

        log("\nBuild informations:");
        for (IMappingFile file : srgs) {
            log(" Maping size: " + file.getClasses().size());
        }
        log("\n");

        BiPredicate<IMethod, IMethod> matchingMethod = (m1, m2) -> (m1 != null && m2 != null
            && m1.getOriginal().equals(m2.getOriginal()) && m1.getDescriptor().equals(m2.getDescriptor()));

        BiPredicate<IField, IField> matchingField = (f1,
            f2) -> (f1 != null && f2 != null && f1.getOriginal().equals(f2.getOriginal()));

        this.outputSrg = new MappingFile();

        MappingFile base = (MappingFile)srgs.get(0);
        //base.reverse();

        for (int n = 1; n < srgs.size(); n++) {
            MappingFile source = (MappingFile)srgs.get(n);

            for (Cls sourceClass : source.getClasses()) {
                Cls baseClass = base.getClass(sourceClass.getOriginal());

                Cls processedClass;
                if (baseClass != null) {
                    processedClass = this.outputSrg.addClass(baseClass.getMapped(), sourceClass.getMapped());
                    log("Added Class " + processedClass);
                } else {
                    String remapped = base.remapClass(sourceClass.getOriginal());
                    if(remapped != null) {
                        processedClass = this.outputSrg.addClass(remapped, sourceClass.getMapped());
                        log("Added remapped Class " + processedClass);
                    } else {
                        error("Cannot resolve class " + sourceClass.getOriginal());
                        processedClass = this.outputSrg.addClass(sourceClass.getOriginal(), sourceClass.getMapped());
                    }
                }

                for (Method sourceMethod : sourceClass.getMethods()) {

                    if (baseClass != null) {

                        Method baseMethod = null;

                        // Find field in base mapping
                        Optional<? extends Method> search = baseClass.getMethods().stream()
                            .filter(m -> matchingMethod.test(m, sourceMethod)).findFirst();
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
                        } else {
                            //error("Base doesnt have method " + sourceMethod);
                        }
                    }

                    //error("Cannot resolve method " + sourceMethod);

                    // Put method mapping from source
                    processedClass.addMethod(sourceMethod.getOriginal(),
                        base.remapDescriptor(sourceMethod.getDescriptor()), sourceMethod.getMapped());
                }

                for (Field sourceField : sourceClass.getFields()) {

                    if (baseClass != null) {

                        Field baseField = null;

                        // Find field in base mapping
                        Optional<? extends Field> s = baseClass.getFields().stream()
                            .filter(f -> matchingField.test(f, sourceField)).findFirst();
                        if (s.isPresent()) {
                            baseField = s.get();
                        }

                        if (baseField != null) {
                            processedClass.addField(baseField.getMapped(), sourceField.getMapped(),
                                baseField.getMappedDescriptor());
                            log("Mapped field " + baseField + " to " + sourceField);
                            continue;
                        } else {
                            //error("Base doesnt have field " + sourceField);
                        }
                    }

                    //error("Cannot resolve field " + sourceField);

                    // Put field mapping from source
                    processedClass.addField(sourceField.getOriginal(), sourceField.getMapped(),
                        sourceField.getDescriptor());
                }
            }
        }

        this.outputSrg.write(this.output, Format.TSRG, false);

        log("Srg mapping done!");
    }
}

class Progress
{

    private int overall = 0;
    private int succeed = 0;
    private int failed = 0;

    public Progress() {
    }

    int getOverallAmount() {
        return overall;
    }

    int getSucceedAmount() {
        return succeed;
    }

    int success() {
        overall++;
        return ++succeed;
    }

    int getFailedAmount() {
        return failed;
    }

    int failed() {
        overall++;
        return ++failed;
    }

    void resetProgress() {
        overall = 0;
        succeed = 0;
        failed = 0;
    }
}
