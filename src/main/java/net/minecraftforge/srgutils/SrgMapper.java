package org.minecraftplus.map;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import net.minecraftforge.srg2source.api.OutputSupplier;
import net.minecraftforge.srg2source.util.io.ConfLogger;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IMappingFile.IClass;
import net.minecraftforge.srgutils.MappingFile;

public class SrgMapper extends ConfLogger<SrgMapper>
{
    private List<IMappingFile> srgs = new ArrayList<>();
//    private Map<Integer, Map<String, String>> clsSrc2Internal = new HashMap<Integer, Map<String, String>>();

    private OutputSupplier output = null;

    public void readSrg(Path srg) {
        try (InputStream in = Files.newInputStream(srg)) {
            IMappingFile map = IMappingFile.load(in);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read SRG: " + srg, e);
        }
    }

    public void setOutput(OutputSupplier value) {
        this.output = value;
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
            throw new IllegalStateException("Missing Range Apply input");
        if (output == null)
            throw new IllegalStateException("Missing Range Apply output");

        log("I'm working!");

        String data = "Test!";

        IMappingFile FIRST = srgs.get(0);

        for (IMappingFile file : srgs) {
            log(" Size: " + file.getClasses().size());
        }
        
        MappingFile outputSrg = new MappingFile();
        
        log(" Before  " + FIRST.getClass(TESTCLASS).toString());

        IMappingFile base = srgs.get(0);
        base.reverse();
        
        log(" Before2 " + FIRST.getClass(TESTCLASS).toString());

        for (int n = 1; n < srgs.size(); n++) {
            IMappingFile source = srgs.get(n);

            for (IClass cls : source.getClasses()) {
                String original = cls.getOriginal();
                String mapped = cls.getMapped();
                
                

                //base.remapClass(source.getClass(original));
            }
        }
        log(" After " + FIRST.getClass(TESTCLASS).toString());

        // write.
        if (data != null) {
            OutputStream outStream = output.getOutput("");
            if (outStream == null)
                throw new IllegalStateException("Could not get output stream form: " + "filePath");
            outStream.write(data.getBytes(StandardCharsets.UTF_8));
            outStream.close();
        }

        output.close();
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
