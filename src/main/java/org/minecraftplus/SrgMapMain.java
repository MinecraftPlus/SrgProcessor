package org.minecraftplus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import org.minecraftplus.api.SrgMapperBuilder;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;
import joptsimple.util.PathConverter;

public class SrgMapMain
{
    private static final ValueConverter<Path> PATH_CONVERTER = new PathConverter();

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        OptionSpec<?> helpArg = parser.acceptsAll(Arrays.asList("h", "help")).forHelp();
        OptionSpec<Path> srgArg = parser.acceptsAll(Arrays.asList("srg", "map", "srgFiles")).withRequiredArg()
            .withValuesConvertedBy(PATH_CONVERTER).required();
        OptionSpec<Path> outArg = parser.acceptsAll(Arrays.asList("out", "output", "outDir")).withRequiredArg()
            .withValuesConvertedBy(PATH_CONVERTER).required();

        try {
            OptionSet options = parser.parse(args);

            if (options.has(helpArg)) {
                parser.printHelpOn(System.out);
                return;
            }

            Path output = options.valueOf(outArg);

            SrgMapperBuilder builder = new SrgMapperBuilder().output(output);

            if (options.has(srgArg)) {
                options.valuesOf(srgArg).forEach(v -> {
                    System.out.println("Srg:     " + v);
                    builder.srg(v);
                });
            }

            System.out.println("Output:  " + output);

            builder.build().run();
        } catch (OptionException e) {
            parser.printHelpOn(System.out);
            e.printStackTrace();
        }
    }

}
