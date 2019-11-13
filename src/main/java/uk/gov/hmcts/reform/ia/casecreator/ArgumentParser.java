package uk.gov.hmcts.reform.ia.casecreator;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class ArgumentParser {

    private final CcdCaseCreator ccdCaseLoader;

    @Autowired
    public ArgumentParser(CcdCaseCreator ccdCaseLoader) {
        this.ccdCaseLoader = ccdCaseLoader;
    }

    public void parse(ApplicationArguments args) throws IOException {
        if (args.getOptionNames().contains("h") || args.getOptionNames().contains("help")) {
            System.out.println("Usage: java -jar sscs-case-creator.jar");
            System.out.println(" --h --help This usage message");
            System.out.println(" --file=[Path to base json file] json file to use as the base of case");
            System.out.println(" --multiple=[number of cases to create] create multiple cases");
            System.out.println(" --headers just prints out auth headers");

            System.exit(0);
        }

        if (hasValue(args, "headers")) {
            System.out.println("\n---------------------------- HEADERS -----------------------");
            ccdCaseLoader.getHeaders();
            System.out.println("------------------------------------------------------------\n");
        } else if (hasValue(args, "load")) {
            String caseId = getOptionalValue(args, "load");
            System.out.println("\n------------------------- CCD case -------------------------");
            ccdCaseLoader.loadCase(caseId);
            System.out.println("------------------------------------------------------------\n");
        } else if (hasValue(args, "loadAll")) {
            System.out.println("\n------------------------- CCD case -------------------------");
            ccdCaseLoader.loadCases();
            System.out.println("------------------------------------------------------------\n");
        }
        else {
            int multiple = Integer.parseInt(getOptionalValue(args, "multiple", "1"));
            String file = getOptionalValue(args, "file");

            for (int counter = 0; counter < multiple; counter++) {
                System.out.println("\n------------------------- CCD case -------------------------");
                ccdCaseLoader.createCase(file);
                System.out.println("------------------------------------------------------------\n");
            }
        }
    }

    private String getOptionalValue(ApplicationArguments args, String name) {
        return getOptionalValue(args, name, null);
    }

    private String getOptionalValue(ApplicationArguments args, String name, String defaultValue) {
        List<String> optionValues = args.getOptionValues(name);
        if (optionValues == null || optionValues.size() == 0) {
            return defaultValue;
        }

        return optionValues.get(0);
    }

    private boolean hasValue(ApplicationArguments args, String name) {
        Set<String> optionNames = args.getOptionNames();

        return optionNames.contains(name);
    }
}
