package org.minecraftplus.srgprocessor.tasks;

import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IRenamer;
import org.minecraftplus.srgprocessor.Utils;
import org.minecraftplus.srgprocessor.tasks.deducer.Action;
import org.minecraftplus.srgprocessor.tasks.deducer.Descriptor;
import org.minecraftplus.srgprocessor.tasks.deducer.Dictionary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Deducer extends SrgWorker<Deducer>
{
    List<Dictionary> dicts = new ArrayList<>();

    public void readDictionary(Path value) {
        try (InputStream in = Files.newInputStream(value)) {
            Dictionary dict = new Dictionary().load(in);
            dicts.add(dict);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read dictionary file: " + value, e);
        }
    }

    @Override
    public void run() throws IOException {
        if (srgs == null)
            throw new IllegalStateException("Missing Srg srgs");
        if (srgs.size() < 1)
            throw new IllegalStateException("Required 1 srgs to process");
        if (outputPath == null)
            throw new IllegalStateException("Missing Srg output");

        Nlog("Informations:");
        for (IMappingFile file : srgs) {
            log(" class count: " + file.getClasses().size());
        }

        Nlog("Processing...");
        IMappingFile input = srgs.get(0);
        this.outputSrg = input.rename(new IRenamer() {
            @Override
            public String rename(IMappingFile.IParameter value) {
                // Deduce parameter name from class type and rules in dictionary
                String deduced = value.getMapped();
                for (Dictionary dictionary : dicts) {
                    deduced = deduceName(value, dictionary);
                }

                // Store used name and add number after if duplicates
                int counter = 1;
                String ret = deduced;
                while (!usedNames.add(ret)) {
                    ret = deduced + String.valueOf(counter);
                    counter++;
                }

                return ret;
            }

            /*
             * Use this workaround to detect when we are processing new method
             *  and can clear list of used parameter names
             */
            @Override
            public String rename(IMappingFile.IMethod value) {
                usedNames.clear();
                return value.getMapped();
            }

            /*
             * Filter then field records to minimize output file
             * TODO DELETE THIS, ONLY FOR DEBBUGING!
             */
            @Override
            public String rename(IMappingFile.IField value) {
                return value.getOriginal();
            }
        });

        this.outputSrg = this.outputSrg.filter(); // TODO DELETE THIS, ONLY FOR DEBBUGING!

        Nlog("Writing to output...");
        write();

        logN("Deducing done!");

        log("Statistics of patter usage:");
        statistics.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> log(String.format("  %8d | %s", e.getValue(), e.getKey())));
    }

    private Set<String> usedNames = new HashSet();
    private Map<String, Integer> statistics = new HashMap();

    public String deduceName(IMappingFile.IParameter parameter, Dictionary dictionary) {
        IMappingFile.IMethod method = parameter.getParent();
        String descriptor = method.getMappedDescriptor();

        List<Descriptor> descriptors = Utils.splitMethodDesc(descriptor);
        Descriptor parameterDescriptor = descriptors.get(parameter.getIndex());

        String parameterName = parameterDescriptor.getName();
        parameterName = parameterName.substring(parameterName.lastIndexOf("/") + 1);
        parameterName = parameterName.substring(parameterName.lastIndexOf("$") + 1);

        // Add 'a' prefix to parameters which are arrays
        if (parameterDescriptor.isArray())
            parameterName = "a" + parameterName;

        for (Map.Entry<Pattern, Action> rule : dictionary.getRules().entrySet()) {
            Matcher matcher = rule.getKey().matcher(parameterName);
            while (matcher.find()) {
                Action action = rule.getValue();
                parameterName = action.act(matcher);

                // Store usage statistics
                String pattern = rule.getKey().pattern();
                Integer stat = statistics.get(pattern);
                if (stat != null) {
                    statistics.replace(pattern, ++stat);
                } else
                    statistics.put(pattern, 1);
            }
        }

        return parameterName.toLowerCase(Locale.ROOT);
    }
}
