package main;

import java.awt.Component;
import java.awt.Container;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;

/**
 * Contains 'back-end' methods
 *
 * @author David C
 */
public class Code {

    // Current GUI frame
    private final JFrame frame;

    // Path strings
    private static String src;
    private static String dest;
    private String lastChoicePathS;
    private Path lastChoicePath;

    // Combo prefix
    public static final String comboPrefix = "combo";

    /**
     * Create a code object
     *
     * @param frame The GUI frame object
     * @param args The arguments passed to the program
     */
    public Code(JFrame frame, String[] args) {

        // Initialize frame
        this.frame = frame;

        // If the number of arguments is invalid
        if (args.length != 2) {

            // Throw error
            String errMsg = "Command Line Args: Source, Destination (Folders)";
            throw new IllegalArgumentException(errMsg);
        }

        // Use arguments to initialize path strings
        src = args[0].replace("_", " ");
        dest = args[1].replace("_", " ");
        lastChoicePathS = dest + "\\lastChoice.txt";
        lastChoicePath = Paths.get(lastChoicePathS);

        // If they are not both folders
        File srcF = new File(src);
        File destF = new File(dest);
        if (!(srcF.isDirectory() && destF.isDirectory())) {

            // Throw error
            throw new IllegalArgumentException(
                    "Source or Dest folder was non-existent");
        }
    }

    /**
     * Run the inputted DOS command and return error output
     *
     * @param command
     */
    private String runCommand(String command) {

        // Create command list
        ArrayList<String> cmdList = new ArrayList<>();
        cmdList.add("cmd");
        cmdList.add("/c");
        cmdList.add(command);

        // Run command
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(cmdList);
            Process p = pb.start();

            // Print command
            System.out.println("command: \n" + command);

            // Get error output
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()));
            String errOut = "";
            String curLine;
            while ((curLine = reader.readLine()) != null) {
                errOut += " " + curLine;
            }

            // If error line is basically empty, notify
            if (errOut.length() < 3) {
                errOut = "None!";
            }

            // Print error output
            System.out.println("error output: " + errOut + "\n");

            // Return error output
            return errOut;

        } catch (IOException e) {

            // Print error info and exit
            System.err.print(e.toString());
            System.exit(1);
        }

        // Return empty
        return "";
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

        // Add day string
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
     * Saves the given string into a text file
     *
     * @param fullChoices Both choices concatenated
     */
    public void saveChoices(String fullChoices) {
        try {

            // If text file exists
            if (choicesExist()) {

                // Delete it 
                Files.delete(lastChoicePath);
            }

            // Recreate the file and write both choices
            Files.writeString(lastChoicePath, fullChoices, CREATE_NEW);

            // Make file hidden
            Files.setAttribute(lastChoicePath, "dos:hidden",
                    Boolean.TRUE, LinkOption.NOFOLLOW_LINKS);

        } catch (IOException e) {

            // Print error info and exit
            System.err.println(e.toString());
            System.exit(1);
        }
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
        initComboBox(options, 1, "Default");

        // Add none option and init right combo box
        String[] none = {"None"};
        String[] options2 = Stream.concat(
                Arrays.stream(none), Arrays.stream(options))
                .toArray(String[]::new);
        initComboBox(options2, 2, "None");
    }

    /**
     * Initialize the options of a given combo box
     *
     * @param options The options as an array of strings
     * @param boxInd Which combo box to initialize (1 or 2)
     * @param def Default option to use when there are no last choices
     */
    public void initComboBox(String[] options, int boxInd, String def) {

        // Turn the options into a combo box model
        JComboBox tempBox = new JComboBox(options);
        ComboBoxModel cbm = tempBox.getModel();

        // If previous choices exist (program has run before)
        if (choicesExist()) {

            // Make holder
            String choiceS = "";

            try {

                // Get both choices as a string
                String full = Files.readString(lastChoicePath);

                // Separate choices and get desired one
                choiceS = full.split(",")[boxInd - 1];

            } catch (IOException ex) {
                Logger.getLogger(Code.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Make the last choice the initial/default selection
            cbm.setSelectedItem((Object) choiceS);

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

        // Retrieve actual combo box and load in options (with default set)
        String CBName = comboPrefix + boxInd;
        ((JComboBox) getComponentByName(CBName)).setModel(cbm);
    }

    /**
     * Retrieve a component by its name
     *
     * @param nameQuery The wanted component's name
     * @return
     */
    public Component getComponentByName(String nameQuery) {

        // Return variable
        Component comp = null;

        // Get all components
        JRootPane jrp = (JRootPane) frame.getComponents()[0];
        Container cp = (Container) jrp.getContentPane();
        JPanel jp = (JPanel) cp.getComponents()[0];
        Component[] parts = jp.getComponents();

        // Iterate over all parts
        for (Component curComp : parts) {

            // When name matches, save and stop
            if (nameQuery.equalsIgnoreCase(curComp.getName())) {
                comp = curComp;
                break;
            }
        }

        // Return component
        return comp;
    }

    /**
     * Rename the template made in the destination
     *
     * @param tempName The template name
     * @param newName The new name
     */
    public void renameTemplate(String tempName, String newName) {

        // Create command string
        String commStr = "rename \"" + dest + tempName + "\" \"" + newName + "\"";

        // Run command and save output
        String errOut = runCommand(commStr);

        // If output indicates duplicate error
        if (errOut.contains("duplicate")) {

            // This means the template already exists,
            // so delete the intermediate file
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
     * Returns true if the last choice file already exists, and false otherwise
     *
     * @return
     */
    private boolean choicesExist() {

        // Attempt to load last choice file
        File lcFile = new File(lastChoicePathS);

        // Return whether it is a valid file
        return lcFile.isFile();
    }

    /**
     * Copy the template from the source to the destination
     *
     * @param tempName
     */
    public void copyTemplate(String tempName) {
        runCommand("copy \"" + src + tempName + "\" \"" + dest + "\"");
    }

}
