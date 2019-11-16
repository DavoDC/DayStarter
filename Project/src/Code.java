
import java.awt.Component;
import java.awt.Container;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.stream.Stream;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;

/**
 * Contains methods to help with GUI processing
 *
 * @author David C
 */
public class Code {

    // Current GUI frame
    private JFrame frame;

    // Directory path
    private static String src;
    private static String dest;

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

        // Save arguments
        src = args[0].replace("_", " ");
        dest = args[1].replace("_", " ");

        // If they are not both folders
        File srcF = new File(src);
        File destF = new File(dest);
        if (!(srcF.isDirectory() && destF.isDirectory())) {

            // Throw error
            throw new IllegalArgumentException("Source or Dest folder was non-existent");
        }
    }

    /**
     * Scan the source directory for templates and load them into a combo box
     *
     * @throws java.io.IOException If path is not found
     */
    public void loadTemplates() throws IOException {

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

        // Put options into combo box
        ComboBoxModel optCBM = new JComboBox(options).getModel();
        ((JComboBox) getComponentByName("tempBut")).setModel(optCBM);
    }

    /**
     * Retrieve a component by its name
     *
     * @param nameQuery
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
     * Generate a new name for a given template copy
     *
     * @param tempName
     * @param tomorrow
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
        result += monthS.substring(0, 1) + monthS.toLowerCase().substring(1, 3);
        result += " ";

        // Add year
        result += year;
        result += " - ";

        // Add type
        String type = tempName.trim().replaceFirst(" ", "").replaceFirst(" ", "");
        result += type.substring(2) + " Plan";

        // Add extension
        result += ".docx";

        // Return result
        return result;
    }

    /**
     * Add an ordinal suffix to a number
     *
     * @param i
     * @return
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
     * Run the inputted DOS command
     *
     * @param command
     */
    private void runCommand(String command) {

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

            // TEST
            // Print info
            System.out.println("command: \n" + command);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            BufferedReader reader2 = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line2;
            while ((line2 = reader2.readLine()) != null) {
                System.out.println(line2);
            }

        } catch (Exception e) {

            // Print error info and exit
            System.err.print(e.toString());
            System.exit(1);
        }

    }

    /**
     * Copy the template from the source to the destination
     *
     * @param tempName
     */
    public void copyTemplate(String tempName) {
        runCommand("copy \"" + src + tempName + "\" \"" + dest + "\"");
    }

    /**
     * Rename the template made in the destination
     *
     * @param tempName
     * @param newName
     */
    public void renameTemplate(String tempName, String newName) {
        runCommand("rename \"" + dest + tempName + "\" \"" + newName + "\"");
    }

}
