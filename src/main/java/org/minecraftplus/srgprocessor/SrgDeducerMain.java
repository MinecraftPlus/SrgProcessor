package org.minecraftplus.srgprocessor;

import joptsimple.*;
import joptsimple.util.PathConverter;
import net.minecraftforge.srgutils.IMappingFile.Format;
import org.minecraftplus.srgprocessor.api.DeducerBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class SrgDeducerMain
{
    private static final ValueConverter<Path> PATH_CONVERTER = new PathConverter();

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        OptionSpec<?> helpArg = parser.acceptsAll(Arrays.asList("h", "help")).forHelp();

        OptionSpec<Path> srgArg = parser.acceptsAll(Arrays.asList("srg", "map", "mapping", "mappingFile")).withRequiredArg()
            .withValuesConvertedBy(PATH_CONVERTER).required();
        OptionSpec<Path> dictArg = parser.acceptsAll(Arrays.asList("dict", "dictionary", "dictionaryFiles")).withRequiredArg()
            .withValuesConvertedBy(PATH_CONVERTER).required();
        OptionSpec<Path> outArg = parser.acceptsAll(Arrays.asList("out", "output", "outDir")).withRequiredArg()
            .withValuesConvertedBy(PATH_CONVERTER).required();
        OptionSpec<Format> formatArg = parser.acceptsAll(Arrays.asList("format", "outputFormat")).withRequiredArg()
            .ofType(Format.class).defaultsTo(Format.TSRG2);
        OptionSpec<Boolean> reverseArg = parser.acceptsAll(Arrays.asList("reverse", "reverseOutput")).withOptionalArg()
            .ofType(Boolean.class).defaultsTo(false);

        try {
            OptionSet options = parser.parse(args);

            if (options.has(helpArg)) {
                parser.printHelpOn(System.out);
                return;
            }

            Path input = options.valueOf(srgArg);
            Path output = options.valueOf(outArg);
            Path dictionary = options.valueOf(dictArg);
            Format outputFormat = options.valueOf(formatArg);
            boolean reverseOutput = options.has(reverseArg) || options.valueOf(reverseArg);

            System.out.println("Input:           " + input);
            System.out.println("Dictionary:      " + dictionary);
            System.out.println("Output:          " + output);
            System.out.println("Output format:   " + outputFormat);
            System.out.println("Reverse output:  " + reverseOutput);

            DeducerBuilder builder = new DeducerBuilder()
                    .input(input).dictionary(dictionary).output(output).format(outputFormat);

            if (reverseOutput)
                builder.reverseOutput();

            builder.build().run();
        } catch (OptionException e) {
            parser.printHelpOn(System.out);
            e.printStackTrace();
        }
    }
}
