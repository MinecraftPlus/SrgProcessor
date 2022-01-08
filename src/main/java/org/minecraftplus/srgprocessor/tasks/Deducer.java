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

public class Deducer extends AbstractWorker<Deducer>
{
    private final List<Dictionary> dicts = new ArrayList<>();
    private boolean collectStats = false;

    public void readDictionary(Path value) {
        try (InputStream in = Files.newInputStream(value)) {
            Dictionary dict = new Dictionary().load(in);
            dicts.add(dict);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read dictionary file: " + value, e);
        }
    }

    public void collectStatistics(boolean collect) { this.collectStats = collect; }

    @Override
    public void run() throws IOException {
        if (srgs == null)
            throw new IllegalStateException("Missing input SRG's");
        if (srgs.size() < 1)
            throw new IllegalStateException("Required one input SRG process");
        if (outputPath == null)
            throw new IllegalStateException("Missing output SRG destination");

        log("Process informations:");
        log(" " + srgs.size() + " input srg(s)");
        for (IMappingFile file : srgs) {
            log("  srg (" + file.getClasses().size() + " classes)");
        }

        if (dicts.size() > 0) {
            log(" " + dicts.size() + " dictionary(ies)");
            for (Dictionary dict : dicts) {
                log("  dict (" + dict.getRules().size() + " rules)");
            }
        } else
            dicts.add(new Dictionary());

        log("Deducing parameters...");
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
                return value.getMapped(); // Don't touch method mapping!!!
            }
        });

        if (!this.dryRun) {
            log("Writing to output...");
            write();
        } else {
            log("No output, dry run used!");
        }

        if (this.collectStats) {
            Nlog("Collected statistics:");
            log(String.format("  %8s | %20s | %8s:%-20s | %s", "count", "trigger", "action",  "value", "package filter"));
            statistics.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> log(String.format("  %8d | %s", e.getValue(), e.getKey())));
        }

        Nlog("Deducing task done!");
    }

    private final Set<String> usedNames = new HashSet<>();
    private final Map<String, Integer> statistics = new HashMap<>();

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
        if (!this.collectStats)
            return; // Don't collect statistics

        String filter = "";
        if (trigger.getFilter() != null)
            filter =  trigger.getFilter().pattern();

        String key = String.format("%20s | %8s:%-20s | %s",
                trigger.getPattern().pattern(), action.getType(), action.getValue(), filter);
        updateStatistics(key);
    }

    private void updateStatistics(String key) {
        if (!this.collectStats)
            return; // Don't collect statistics

        Integer stat = statistics.get(key);
        if (stat != null) {
            statistics.replace(key, ++stat);
        } else
            statistics.put(key, 1);
    }
}
