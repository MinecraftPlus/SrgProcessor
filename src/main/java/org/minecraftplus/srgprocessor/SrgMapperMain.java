package org.minecraftplus.srgprocessor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import org.minecraftplus.srgprocessor.api.MapperBuilder;
import org.minecraftplus.srgprocessor.tasks.Mapper.Mode;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;
import joptsimple.util.PathConverter;
import net.minecraftforge.srgutils.IMappingFile.Format;

public class SrgMapperMain
{
    private static final ValueConverter<Path> PATH_CONVERTER = new PathConverter();

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        OptionSpec<?> helpArg = parser.acceptsAll(Arrays.asList("h", "help")).forHelp();

        OptionSpec<Mode> modeArg = parser.acceptsAll(Arrays.asList("mode", "jobMode")).withRequiredArg()
            .ofType(Mode.class).required();
        OptionSpec<Path> srgArg = parser.acceptsAll(Arrays.asList("srg", "map", "srgFiles")).withRequiredArg()
            .withValuesConvertedBy(PATH_CONVERTER).required();
        OptionSpec<Path> outArg = parser.acceptsAll(Arrays.asList("out", "output", "outDir")).withRequiredArg()
            .withValuesConvertedBy(PATH_CONVERTER).required();
        OptionSpec<Format> formatArg = parser.acceptsAll(Arrays.asList("format", "outputFormat")).withRequiredArg()
            .ofType(Format.class).defaultsTo(Format.TSRG2);
        OptionSpec<Boolean> filterArg = parser.acceptsAll(Arrays.asList("filter", "filterSame")).withOptionalArg()
            .ofType(Boolean.class).defaultsTo(false);
        OptionSpec<Boolean> reverseArg = parser.acceptsAll(Arrays.asList("reverse", "reverseOutput")).withOptionalArg()
            .ofType(Boolean.class).defaultsTo(false);

        try {
            OptionSet options = parser.parse(args);

            if (options.has(helpArg)) {
                parser.printHelpOn(System.out);
                return;
            }

            Mode jobMode = options.valueOf(modeArg);
            Path output = options.valueOf(outArg);
            Format outputFormat = options.valueOf(formatArg);
            boolean filterSameNames = options.has(filterArg) || options.valueOf(filterArg);
            boolean reverseOutput = options.has(reverseArg) || options.valueOf(reverseArg);

            MapperBuilder builder = new MapperBuilder().mode(jobMode).output(output).format(outputFormat);

            System.out.println("Mode:     " + jobMode);

            if (options.has(srgArg)) {
                options.valuesOf(srgArg).forEach(v -> {
                    System.out.println("Srg:     " + v);
                    builder.srg(v);
                });
            }

            System.out.println("Output:  " + output);
            System.out.println("Output format:  " + outputFormat);
            System.out.println("Filter:  " + filterSameNames);
            System.out.println("Reverse output:  " + reverseOutput);

            if (reverseOutput)
                builder.reverseOutput();

            builder.build().run();
        } catch (

        OptionException e) {
            parser.printHelpOn(System.out);
            e.printStackTrace();
        }
    }

}
