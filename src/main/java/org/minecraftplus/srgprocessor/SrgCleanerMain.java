package org.minecraftplus.srgprocessor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;
import joptsimple.util.PathConverter;
import net.minecraftforge.srgutils.IMappingFile.Format;
import org.minecraftplus.srgprocessor.api.CleanerBuilder;

public class SrgCleanerMain
{
    private static final ValueConverter<Path> PATH_CONVERTER = new PathConverter();

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        OptionSpec<?> helpArg = parser.acceptsAll(Arrays.asList("h", "help")).forHelp();
        OptionSpec<Path> inArg = parser.acceptsAll(Arrays.asList("in", "input", "srgFile")).withRequiredArg()
                .withValuesConvertedBy(PATH_CONVERTER).required();
        OptionSpec<Path> outArg = parser.acceptsAll(Arrays.asList("out", "output", "outFile")).withRequiredArg()
                .withValuesConvertedBy(PATH_CONVERTER).required();
        OptionSpec<Format> formatArg = parser.acceptsAll(Arrays.asList("f", "format", "outputFormat")).withRequiredArg()
                .ofType(Format.class).defaultsTo(Format.TSRG2);
        OptionSpec<Boolean> reverseArg = parser.acceptsAll(Arrays.asList("r", "reverse", "reverseOutput")).withOptionalArg()
                .ofType(Boolean.class).defaultsTo(false);
        OptionSpec<Boolean> filterArg = parser.acceptsAll(Arrays.asList("filter", "filterSameNames"),
                        "Nodes with same original and mapped name will be removed from output").withOptionalArg()
                .ofType(Boolean.class).defaultsTo(false);
        OptionSpec<Boolean> inferArg = parser.acceptsAll(Arrays.asList("infer", "inferMethodParameters"),
                        "Method parameters will be inferred from method descriptor").withOptionalArg()
                .ofType(Boolean.class).defaultsTo(false);

        try {
            OptionSet options = parser.parse(args);

            if (options.has(helpArg)) {
                parser.printHelpOn(System.out);
                return;
            }

            Path input = options.valueOf(inArg);
            Path output = options.valueOf(outArg);
            Format outputFormat = options.valueOf(formatArg);
            boolean reverseOutput = options.has(reverseArg) || options.valueOf(reverseArg);
            boolean filter = options.has(filterArg) || options.valueOf(filterArg);
            boolean inferParameters = options.has(inferArg);

            System.out.println("Input :    " + input);
            System.out.println("Output:    " + output);
            System.out.println("Format:    " + outputFormat);
            System.out.println("Reverse:   " + reverseOutput);
            System.out.println("Filter:    " + filter);
            System.out.println("Infer:     " + inferParameters);

            CleanerBuilder builder = new CleanerBuilder()
                    .input(input).output(output).format(outputFormat)
                    .infer(inferParameters).filter(filter);

            if (reverseOutput)
                builder.reverseOutput();

            builder.build().run();
        } catch (OptionException e) {
            parser.printHelpOn(System.out);
            e.printStackTrace();
        }
    }
}
