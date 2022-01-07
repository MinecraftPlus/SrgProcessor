package org.minecraftplus.srgprocessor.tasks;

import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IRenamer;
import org.minecraftplus.srgprocessor.Utils;
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

        if (dicts.size() > 0) {
            for (Dictionary dict : dicts) {
                log("  dictionary: " + dict.getRules().size());
            }
        } else
            dicts.add(new Dictionary());

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

        if (!this.dryRun) {
            Nlog("Writing to output...");
            write();
        } else {
            Nlog("No output, dry run used!");
        }

        logN("Deducing done!");

        log("Statistics of patter usage:");
        statistics.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> log(String.format("  %8d | %s", e.getValue(), e.getKey())));
    }

    private Set<String> usedNames = new HashSet();
    private Map<String, Integer> statistics = new HashMap();

    public String deduceName(IMappingFile.IParameter parameter, Dictionary dictionary) {
        // Spy how much parameters were processed
        updateStatistics("parameters_processed");

        IMappingFile.IMethod method = parameter.getParent();
        List<Descriptor> descriptors = Utils.splitMethodDesc(method.getMappedDescriptor());
        Descriptor parameterDescriptor = descriptors.get(parameter.getIndex());

        String parameterName = parameterDescriptor.getName();
        parameterName = parameterName.substring(parameterName.lastIndexOf("/") + 1);
        parameterName = parameterName.substring(parameterName.lastIndexOf("$") + 1);

        // Add 'a' prefix to parameters which are arrays
        if (parameterDescriptor.isArray())
            parameterName = "a" + parameterName;

        for (Map.Entry<Dictionary.Trigger, Dictionary.Action> rule : dictionary.getRules().entrySet()) {
            Dictionary.Trigger trigger = rule.getKey();

            Pattern filter = trigger.getFilter();
            if (filter != null && !filter.matcher(parameterDescriptor.getName()).matches()) {
                continue; // Skip dictionary replaces if filter not pass
            }

            Pattern pattern = trigger.getPattern();
            Dictionary.Action action = rule.getValue();
            Matcher matcher = pattern.matcher(parameterName);
            if (matcher.matches()) { // Only one replace at time
                parameterName = action.act(matcher);
                updateStatistics(trigger, action);
            }
        }

        // Store long parameters statistics
        if (parameterName.length() > 15)
            updateStatistics("parameters_longer_than_15");

        return parameterName.toLowerCase(Locale.ROOT);
    }

    private void updateStatistics(Dictionary.Trigger trigger, Dictionary.Action action) {
        String filter = "";
        if (trigger.getFilter() != null)
            filter =  trigger.getFilter().pattern();

        String key = String.format("%20s | %8s:%-20s | %s",
                trigger.getPattern().pattern(), action.getType(), action.getValue(), filter);
        updateStatistics(key);
    }

    private void updateStatistics(String key) {
        Integer stat = statistics.get(key);
        if (stat != null) {
            statistics.replace(key, ++stat);
        } else
            statistics.put(key, 1);
    }
}
