package main;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JToggleButton;

/**
 * Contains 'back-end' methods
 *
 * @author David C
 */
public class Code {

    // Configuration paths
    public static String configFolder;
    private static String configPathS;

    // Config settings
    private final String configFileName = GUI.PROG_DESC + "Config.ini";
    private final String configSep = ",,,";
    private final String configFormat = "TemplateFolder" + configSep + "DestFolder";
    private final int configLines = 2;

    // Template folder path (source)
    private static String tempFolder;

    // Destination folder path (holds copies)
    private static String destFolder;

    // Swing session handler
    private SessionHandler sessH;

    /**
     * Create a code object
     */
    public Code() {

        // Notify
        System.out.println("\nWelcome to DayStarter!\n");

        // Try to load config file
        try {

            // Initialize config folder path
            configFolder = System.getProperty("java.io.tmpdir");

            // Don't put in temp
            configFolder = configFolder.replace("\\Temp", "");

            // Put in DayStarter folder
            configFolder += "DayStarter";

            // If config folder doesn't exist
            if (!isFolder(configFolder)) {

                // Make config folder
                Files.createDirectories(Paths.get(configFolder));

                // Notify
                System.out.println("Made config folder: "
                        + quote(configFolder));
            }

            // Initialize config file path string and object
            configPathS = configFolder + "\\" + configFileName;
            Path configPathO = Paths.get(configPathS);

            // If config file doesn't exist
            if (!isFile(configPathS)) {

                // Make dummy/default config file
                String myTemp = "C:\\Users\\David\\Google Drive"
                        + "\\Tasks\\Templates\\Uni Time";
                String myDest = "C:\\Users\\David\\Google Drive\\Tasks";
                String defDirs = myTemp + configSep + myDest;
                Files.writeString(configPathO, defDirs);

                // Notify
                System.out.println("Made dummy/default config file: "
                        + quote(configPathS));
                System.out.println("Please edit file if needed,");
                System.out.println("and run the program again.");

                // Throw error 
                String msg = "\nUninitialized config file";
                throw new IllegalArgumentException(msg);
            }

            // Extract config parts
            String configFull = Files.readString(configPathO);
            String[] configParts = configFull.split(configSep);

            // If the number of parts is invalid
            if (configParts.length != configLines) {

                // Throw error
                String msg = "\nWrong format";
                throw new IllegalArgumentException(msg);
            }

            // Use arguments to initialize path strings
            tempFolder = configParts[0].trim();
            destFolder = configParts[1].trim();

            // If template folder is invalid
            if (!isFolder(tempFolder)) {

                // Throw error
                String msg = "\nTemplate folder doesn't exist.\n";
                msg += quote(tempFolder) + " is not a valid folder path.";
                throw new IllegalArgumentException(msg);
            }

            // If destination folder is invalid
            if (!isFolder(destFolder)) {

                // Throw error
                String msg = "\nDestination folder doesn't exist.\n";
                msg += quote(destFolder) + " is not a valid folder path.";
                throw new IllegalArgumentException(msg);
            }

        } catch (IOException | IllegalArgumentException e) {

            // If there are any issues with configuration:
            // Notify (with System.out for better ordering)
            System.out.println("\nConfiguration issue!");
            System.out.println("\nSpecific issue: " + e.getMessage());

            System.out.println("\nA configuration file");
            System.out.println("with the properties below is expected");
            System.out.println("Path: " + configPathS);
            System.out.println("Format: " + configFormat);
            System.out.println("i.e. Format is:");
            System.out.println("1st line = Folder holding templates");
            System.out.println("2nd line = Separator (" + configSep + ")");
            System.out.println("3rd line = Destination folder for copies");

            // Exit with error
            System.exit(1);
        }

        // Notify
        System.out.println("Configuration successful!");

        // Try to scan the source directory for templates,
        // and load them into the combo boxes
        try {

            // Get paths of the Word documents in the source folder
            Stream<Path> walk = Files.walk(Paths.get(tempFolder));
            Object[] optionsRaw = walk.map(x -> x.toString()).
                    filter(f -> f.endsWith(".docx")).toArray();

            // If there are no templates in template folder
            if (optionsRaw.length == 0) {
                
                // Notify
                System.out.println("\nTemplate folder empty!");
                System.out.println("\nStart button disabled!");
                
                // Disable start button
                GUI.gui.getComponentByName("startBut").setEnabled(false);
            }

            // Convert the file paths to file name strings
            String[] options = new String[optionsRaw.length];
            int index = 0;
            for (Object ob : optionsRaw) {

                // Get current string
                String curS = (String) ob;

                // Refine it
                curS = curS.replace(tempFolder + "\\", "");
                curS = curS.replace("T.docx", "");

                // Add it to the list
                options[index] = curS;
                index++;
            }

            // Initialize left combo box
            initComboBox(options, 0, "Default");

            // Add none option 
            String[] none = {"None"};
            String[] options2 = Stream.concat(
                    Arrays.stream(none), Arrays.stream(options))
                    .toArray(String[]::new);

            // Initialize right combo box
            initComboBox(options2, 1, "None");
        } catch (IOException ex) {
            // This is impossible as the src path has been checked
        }
    }

    /**
     * Generate a new name for a given template copy
     *
     * @param tempName
     * @param tomorrow
     * @return
     */
    public String getNewName(String tempName, boolean tomorrow) {

        // Get current date
        LocalDate date = LocalDate.now();

        // If tomorrow wanted
        if (tomorrow) {

            // Shift one day forward
            date = date.plusDays(1);
        }

        // Result string
        String result = "";

        // Add day name in sentence case
        String fullyCapitalizedDay = date.getDayOfWeek().toString();
        result += getSentenceCase(fullyCapitalizedDay);
        result += " ";

        // Add day number string
        int day = date.getDayOfMonth();
        result += getOrdinal(day);
        result += " ";

        // Add month string
        int month = date.getMonthValue();
        String monthS = Month.of(month).name();
        result += monthS.substring(0, 1);
        result += monthS.toLowerCase().substring(1, 3);
        result += " ";

        // Add year
        result += date.getYear();
        result += " - ";

        // Add type
        String type = tempName.trim();
        type = type.replaceFirst(" ", "").replaceFirst(" ", "");
        result += type.substring(2) + " Plan";

        // Add extension
        result += ".docx";

        // Return result
        return result;
    }

    /**
     * Initialize the options of a given combo box
     *
     * @param options The options as an array of strings
     * @param boxInd Which combo box to initialize (1 or 2)
     * @param def Default option to use when there are no last choices
     */
    public final void initComboBox(String[] options, int boxInd, String def) {

        // If session handler is not initialized
        if (sessH == null) {

            // Initialize session handler,
            // so previous options may be loaded
            sessH = new SessionHandler();
        }

        // Get name of combo box
        String cbName = SessionHandler.compNames[boxInd];

        // Turn the options into a combo box model
        JComboBox<String> tempBox;
        tempBox = new JComboBox<>(options);
        ComboBoxModel<String> cbm = tempBox.getModel();

        // If previous choices exist (program has run before)
        if (sessH.doesPrevSessExist()) {

            // Get previous session
            Session prevDSS = sessH.getPrevSess();

            // Get previous instance of combo box
            JComboBox prevJCB;
            prevJCB = (JComboBox) prevDSS.getComp(cbName);

            // Extract previous selection
            String prevSel;
            prevSel = (String) prevJCB.getSelectedItem();

            // If previous template still exists
            if (isValidTemplate(prevSel)) {

                // Make the previous choice the default selection
                cbm.setSelectedItem(prevSel);
            }
        } else {

            // Else if there is no previous session / last choice:
            // If default selection wanted
            if (def.equalsIgnoreCase("Default")) {

                // Set first option as initial/default option
                cbm.setSelectedItem(cbm.getElementAt(0));

            } else {

                // Otherwise, make inputted string the default option
                cbm.setSelectedItem(def);
            }
        }

        // Retrieve actual combo box and update options + default
        ((JComboBox<String>) GUI.gui.getComponentByName(cbName)).setModel(cbm);
    }

    /**
     * Process a given combo box
     *
     * @param boxComp
     */
    public void processComboBox(Component boxComp) {

        // Extract combo box selection
        JComboBox jcb = (JComboBox) boxComp;
        String tempS = (String) jcb.getModel().getSelectedItem();

        // Do not process further is selection is none
        if (tempS.equalsIgnoreCase("None")) {
            return;
        }

        // Infer template name
        String tempName = "\\" + tempS + "T.docx";

        // Copy template from source to destination
        copyTemplate(tempName);

        // Get tomorrow status
        JToggleButton tmrwBut;
        tmrwBut = (JToggleButton) GUI.gui.getComponentByName("tmrwBut");
        boolean tmrwOn = tmrwBut.isSelected();

        // Get new name for template
        String newName = getNewName(tempS, tmrwOn);

        // Rename template made
        renameTemplate(tempName, newName);

        // Open template
        openTemplate(newName);
    }

    /**
     * Rename the template that was copied to the destination
     *
     * @param tempName The template name
     * @param newName The new name
     */
    public void renameTemplate(String tempName, String newName) {

        // Create and run command
        String progName = "rename";
        String[] args = new String[2];
        args[0] = quote(destFolder + tempName);
        args[1] = quote(newName);
        Command comm = new Command(progName, args);
        comm.run();

        // If output indicates duplicate error
        if (comm.getErrOutput().contains("duplicate")) {

            // This means the template already exists,
            // so delete the intermediate (unrenamed) file
            try {
                Files.delete(Paths.get(destFolder + tempName));
                System.out.println("Deleted duplicate intermediate file");

            } catch (IOException ex) {
                Logger.getLogger(Code.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Get string with first letter capitalized and the rest lower case
     *
     * @param input
     * @return
     */
    private String getSentenceCase(String input) {

        // Holder
        String output = "";

        // For every character in string
        for (int i = 0; i < input.length(); i++) {

            // Get current char as String
            String curCharS = "" + input.charAt(i);

            // If first character
            if (i == 0) {

                // Add as upper case
                output += curCharS.toUpperCase();
            } else {

                // Else if not first,
                // add as lower case
                output += curCharS.toLowerCase();
            }
        }

        // Return output
        return output;
    }

    /**
     * Add an ordinal suffix to a number
     *
     * @param i
     * @return a string with ordinal suffix added
     */
    private String getOrdinal(int i) {
        int mod100 = i % 100;
        int mod10 = i % 10;
        if (mod10 == 1 && mod100 != 11) {
            return i + "st";
        } else if (mod10 == 2 && mod100 != 12) {
            return i + "nd";
        } else if (mod10 == 3 && mod100 != 13) {
            return i + "rd";
        } else {
            return i + "th";
        }
    }

    /**
     * Copy the template from the source to the destination
     *
     * @param tempName
     */
    public void copyTemplate(String tempName) {

        // Create and run command
        String progName = "copy";
        String[] args = new String[2];
        args[0] = quote(tempFolder + tempName);
        args[1] = quote(destFolder);
        Command comm = new Command(progName, args);
        comm.run();
    }

    /**
     * Open a given template
     *
     * @param tempName
     */
    public void openTemplate(String tempName) {

        // Create and run command
        String progName = "explorer.exe";
        String[] args = new String[1];
        args[0] = quote(destFolder + "\\" + tempName);
        Command comm = new Command(progName, args);
        comm.run();
    }

    /**
     * Process selections
     */
    public void procSel() {

        // Extract/Save session
        sessH.save();

        // Do actions associated with selections
        Session dss = sessH.getPrevSess();
        processComboBox(dss.getComp(SessionHandler.compNames[0]));
        processComboBox(dss.getComp(SessionHandler.compNames[1]));
    }

    /**
     * Returns true if the given template exists
     *
     * @param prevSel
     * @return
     */
    private boolean isValidTemplate(String prevSel) {

        // Create full path
        String fullPath = tempFolder + "\\";
        fullPath += prevSel + "T.docx";

        // Return true if file exists
        return isFile(fullPath);
    }

    /**
     * Returns true if given path is a file and false otherwise
     *
     * @param filePath
     * @return
     */
    public static boolean isFile(String filePath) {

        // Return true if path is a file
        return (new File(filePath)).isFile();
    }

    /**
     * Returns true if given path is a folder and false otherwise
     *
     * @param folderPath
     * @return
     */
    public static boolean isFolder(String folderPath) {

        // Return true if path is a directory
        return (new File(folderPath)).isDirectory();
    }

    /**
     * Returns a given string with quotations
     *
     * @param s
     * @return
     */
    public static String quote(String s) {
        return "\"" + s + "\"";
    }
}
