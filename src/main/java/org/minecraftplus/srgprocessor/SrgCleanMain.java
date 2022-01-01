package org.minecraftplus.srgprocessor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;
import joptsimple.util.EnumConverter;
import joptsimple.util.PathConverter;
import net.minecraftforge.srgutils.IMappingBuilder;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IMappingFile.Format;
import net.minecraftforge.srgutils.IMappingFile.IParameter;

public class SrgCleanMain
{
    private static final ValueConverter<Path> PATH_CONVERTER = new PathConverter();
    private static final ValueConverter<Format> FORMAT_CONVERTER = new FormatConverter(Format.class);

    public static final Pattern DESC = Pattern.compile("L([^;]+);|([ZBCSIFDJ])");

    private static IMappingFile srg;


    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        OptionSpec<?> helpArg = parser.acceptsAll(Arrays.asList("h", "help")).forHelp();
        OptionSpec<Path> inArg = parser.acceptsAll(Arrays.asList("in", "input", "srgFile")).withRequiredArg()
            .withValuesConvertedBy(PATH_CONVERTER).required();
        OptionSpec<Path> outArg = parser.acceptsAll(Arrays.asList("out", "output", "outFile")).withRequiredArg()
            .withValuesConvertedBy(PATH_CONVERTER).required();
        OptionSpec<Format> formatArg = parser.acceptsAll(Arrays.asList("f", "format")).withRequiredArg()
            .withValuesConvertedBy(FORMAT_CONVERTER).defaultsTo(Format.TSRG2);
        OptionSpec<Boolean> reverseArg = parser.acceptsAll(Arrays.asList("r", "reverse")).withOptionalArg()
            .ofType(Boolean.class).defaultsTo(false);
        OptionSpec<Boolean> filterArg = parser.acceptsAll(Arrays.asList("filter")).withOptionalArg()
            .ofType(Boolean.class).defaultsTo(false);
        OptionSpec<Void> inferParametersArg = parser.acceptsAll(Arrays.asList("inferMethodParameters"), "Method parameters will be inferred from method descriptor");

        try {
            OptionSet options = parser.parse(args);

            if (options.has(helpArg)) {
                parser.printHelpOn(System.out);
                return;
            }

            Path input = options.valueOf(inArg);
            Path output = options.valueOf(outArg);
            Format format = options.valueOf(formatArg);
            boolean reverse = options.has(reverseArg) || options.valueOf(reverseArg);
            boolean filter = options.has(filterArg) || options.valueOf(filterArg);
            boolean inferParameters = options.has(inferParametersArg);

            System.out.println("Input :    " + input);
            System.out.println("Output:    " + output);
            System.out.println("Format:    " + format);
            System.out.println("Reverse:   " + reverse);
            System.out.println("Filter:    " + filter);
            System.out.println("Infer:     " + inferParameters);

            if (filter && inferParameters) {
                System.out.println("# Infering parameters and filtering at the same moment can limit class and");
                System.out.println("# methods filtering capabilities due to generated parameters are not equal!");
            }

            System.out.println("Reading input SRG...");
            readSrg(input);
            
            if (inferParameters) {
                System.out.println("Infering method parameters...");
                IMappingBuilder builder = IMappingBuilder.create();

                srg.getClasses().stream().forEach(cls -> {
                    IMappingBuilder.IClass builderCls = builder.addClass(cls.getOriginal(), cls.getMapped());
                    cls.getFields().stream().forEach(fld -> {
                        IMappingBuilder.IField builderField =  builderCls.field(fld.getOriginal(), fld.getMapped());
                        builderField.descriptor(fld.getDescriptor()); // TODO Ugly solution to adding description to field
                    });
                    cls.getMethods().stream().forEach(mtd -> {
                        IMappingBuilder.IMethod builderMtd = builderCls.method(mtd.getDescriptor(), mtd.getOriginal(), mtd.getMapped());

                        ArrayList<String> parameters = splitMethodDesc(mtd.getDescriptor());
                        for(int parNumber = 0; parNumber < parameters.size(); parNumber++ ) {
                            IParameter parameter = mtd.getParameter(parNumber);
                            if(parameter == null) {
                                String parent = mtd.getMapped();

                                if (parent.equals("<init>")) { // When mapping constructor use class name
                                    String a = mtd.getParent().getMapped();
                                    a = a.substring(a.lastIndexOf("/") + 1);
                                    a = a.substring(a.lastIndexOf("$") + 1);
                                    parent = parent.replaceAll("<init>", a);
                                }

                                String inferredName = "p_" + parent + "_" + (parNumber + 1);
                                builderMtd.parameter(parNumber, "inferred", inferredName);
                            }
                        }
                    });
                });
                srg = builder.build().getMap("left", "right");
            }

            if (filter) {
                System.out.println("Filtering...");
                srg = srg.filter();
            }
  
            System.out.println("Writing out SRG...");
            writeSrg(output, format, reverse);

            System.out.println("Done!");

        } catch (OptionException e) {
            parser.printHelpOn(System.out);
            e.printStackTrace();
        }
    }

    public static class FormatConverter extends EnumConverter<Format>
    {
        protected FormatConverter(Class<Format> clazz) {
            super(clazz);
        }

        @Override
        public Format convert(final String value) {
            Format format = Format.get(value);

            if (format == null) {
                throw new IllegalArgumentException(
                    String.format("String value '%s' must match '%s'.", value, valuePattern()));
            }

            return format;
        }

        @Override
        public Class<Format> valueType() {
            return Format.class;
        }

        @Override
        public String valuePattern() {
            return "/(?:srg|xsrg|csrg|tsrg|tsrg2|pg|tiny1|tiny)/";
        }
    }

    public static ArrayList<String> splitMethodDesc(String desc) {
        int beginIndex = desc.indexOf('(');
        int endIndex = desc.lastIndexOf(')');
        if((beginIndex == -1 && endIndex != -1) || (beginIndex != -1 && endIndex == -1)) {
            System.err.println(beginIndex);
            System.err.println(endIndex);
            throw new RuntimeException();
        }
        String x0;
        if(beginIndex == -1 && endIndex == -1) {
            x0 = desc;
        }
        else {
            x0 = desc.substring(beginIndex + 1, endIndex);
        }

        Matcher matcher = DESC.matcher(x0);
        ArrayList<String> listMatches = new ArrayList<>();
        while(matcher.find()) {
            listMatches.add(matcher.group());
        }
        return listMatches;
    }

    public static void readSrg(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            srg = IMappingFile.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read SRG: " + path, e);
        }
    }

    public static void writeSrg(Path path, Format format, boolean reversed) {
        try {
            srg.write(path, format, reversed);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write SRG: " + path, e);
        }
    }

}
