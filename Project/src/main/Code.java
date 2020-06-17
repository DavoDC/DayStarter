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

    // Configuration
    private final String configPathS = ".//" + GUI.PROGRAM + "Config.ini";
    private final String configFormat = "TemplateFolder,,,DestFolder";

    // Path strings
    private static String src;
    private static String dest;

    // Swing session
    private SessionHandler sessH;

    /**
     * Create a code object
     */
    public Code() {

        // Attempt to load config file 
        Path configPath = Paths.get(configPathS);
        String configFull = "";
        try {
            configFull = Files.readString(configPath);
        } catch (IOException e) {

            // Print error info and exit
            System.err.println("\nNo config file found. A configuration file");
            System.err.println("with the properties below is expected");
            System.err.println("Path: " + configPathS);
            System.err.println("Format: " + configFormat);
            System.exit(1);
        }

        // Extract config parts
        String[] configParts = configFull.split(",,,");

        // If the number of parts is invalid
        if (configParts.length != 2) {

            // Throw error
            String msg = "Format should be = " + configFormat;
            throw new IllegalArgumentException(msg);
        }

        // Use arguments to initialize path strings
        src = configParts[0].trim();
        dest = configParts[1].trim();

        // If they are not both folders
        File srcF = new File(src);
        File destF = new File(dest);
        if (!(srcF.isDirectory() && destF.isDirectory())) {

            // Throw error
            String msg = "Template or dest. folder doesn't exist";
            throw new IllegalArgumentException(msg);
        }

        // Initialize Swing Session
        sessH = new SessionHandler(dest);
    }

    /**
     * Generate a new name for a given template copy
     *
     * @param tempName
     * @param tomorrow
     * @return
     */
    public String getNewName(String tempName, boolean tomorrow) {

        // Get date information
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        // Add one to day if tomorrow is wanted
        if (tomorrow) {
            day++;
        }

        // Result string
        String result = "";

        // Get day name in sentence case
        String upperDay = today.getDayOfWeek().toString();
        String normalDay = "";
        for (int i = 0; i < upperDay.length(); i++) {
            // Get current char as String
            String curCharS = "" + upperDay.charAt(i);

            // Add to final
            // If first, add as is
            if (i == 0) {
                normalDay += curCharS;
            } else {
                // Else if not first,
                // add as lower case
                normalDay += curCharS.toLowerCase();
            }
        }

        // Add day name string
        result += normalDay;
        result += " ";

        // Add day number string
        result += getOrdinal(day);
        result += " ";

        // Add month string
        String monthS = Month.of(month).name();
        result += monthS.substring(0, 1);
        result += monthS.toLowerCase().substring(1, 3);
        result += " ";

        // Add year
        result += year;
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
     * Scan the source directory for templates, and load them into the combo
     * boxes
     *
     * @throws java.io.IOException If path is not found
     */
    public void loadOptions() throws IOException {

        // Get paths of the Word documents in the source folder
        Stream<Path> walk = Files.walk(Paths.get(src));
        Object[] optionsRaw = walk.map(x -> x.toString()).
                filter(f -> f.endsWith(".docx")).toArray();

        // Convert the file paths to file name strings
        String[] options = new String[optionsRaw.length];
        int index = 0;
        for (Object ob : optionsRaw) {

            // Get current string
            String curS = (String) ob;

            // Refine it
            curS = curS.replace(src + "\\", "");
            curS = curS.replace("T.docx", "");

            // Add it to the list
            options[index] = curS;
            index++;
        }

        // Initialize left combo box
        initComboBox(options, 0, "Default");

        // Add none option and init right combo box
        String[] none = {"None"};
        String[] options2 = Stream.concat(
                Arrays.stream(none), Arrays.stream(options))
                .toArray(String[]::new);
        initComboBox(options2, 1, "None");
    }

    /**
     * Initialize the options of a given combo box
     *
     * @param options The options as an array of strings
     * @param boxInd Which combo box to initialize (1 or 2)
     * @param def Default option to use when there are no last choices
     */
    public void initComboBox(String[] options, int boxInd, String def) {

        // Get name of combo box
        String cbName = SessionHandler.compNames[boxInd];

        // Turn the options into a combo box model
        JComboBox tempBox = new JComboBox(options);
        ComboBoxModel cbm = tempBox.getModel();

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
            if (isValidTemplate(prevSel)) // Refine it
            {
                // Make the previous choice the default selection
                cbm.setSelectedItem(prevSel);
            }
        } else {

            // Else if there is no last choice:
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
        ((JComboBox) GUI.gui.getComponentByName(cbName))
                .setModel(cbm);
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
        boolean shortTmrwText = tmrwBut.getText().length() < 6;

        // Get new name for template
        String newName = getNewName(tempS, shortTmrwText);

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
        args[0] = quote(dest + tempName);
        args[1] = quote(newName);
        Command comm = new Command(progName, args);
        comm.run();

        // If output indicates duplicate error
        if (comm.getErrOutput().contains("duplicate")) {

            // This means the template already exists,
            // so delete the intermediate (unrenamed) file
            try {
                Files.delete(Paths.get(dest + tempName));
                System.out.println("Deleted duplicate intermediate file");

            } catch (IOException ex) {
                Logger.getLogger(Code.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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
        args[0] = quote(src + tempName);
        args[1] = quote(dest);
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
        args[0] = quote(dest + "\\" + tempName);
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
        String fullPath = src + "\\";
        fullPath += prevSel + "T.docx";

        // Return true if file exists
        return (new File(fullPath)).isFile();
    }

    /**
     * Returns a given string with quotations
     *
     * @param s
     * @return
     */
    private String quote(String s) {
        return "\"" + s + "\"";
    }
}
