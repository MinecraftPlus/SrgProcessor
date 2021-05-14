package org.minecraftplus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;
import joptsimple.util.EnumConverter;
import joptsimple.util.PathConverter;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IMappingFile.Format;

public class SrgCleanMain
{
    private static final ValueConverter<Path> PATH_CONVERTER = new PathConverter();
    private static final ValueConverter<Format> FORMAT_CONVERTER = new FormatConverter(Format.class);

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

            System.out.println("Input :  " + input);
            System.out.println("Output:  " + output);
            System.out.println("Format:  " + format);
            System.out.println("Reverse: " + reverse);
            System.out.println("Filter:  " + filter);

            System.out.println("Reading input SRG...");
            readSrg(input);
            
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
