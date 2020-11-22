package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles session I/O
 *
 * @author David
 */
public class SessionHandler {

    // Name of session file
    private final String fileNameS = GUI.PROG_DESC + "SessionInstance.txt";

    // Component names
    public static final String[] compNames = {"combo1", "combo2"};

    // Full path to session file
    private final String fullPathS;
    private final Path pathObj;

    /**
     * Initialize
     *
     * @param dest
     */
    public SessionHandler(String dest) {

        // Initialize path objects
        fullPathS = dest + "\\" + fileNameS;
        pathObj = Paths.get(fullPathS);
    }

    /**
     * Save the session
     */
    public void save() {
        try {

            // If a previous session file exists
            if (doesPrevSessExist()) {

                // Delete it because we are saving a new session
                Files.delete(pathObj);
            }

            // Create session
            Session sess = new Session(GUI.gui, compNames);

            // Write session to file
            writeSession(sess);

            // Make file hidden
            Files.setAttribute(pathObj, "dos:hidden", true,
                    LinkOption.NOFOLLOW_LINKS);

        } catch (IOException e) {

            // Print error info and exit
            System.err.print(e.toString());
            System.exit(1);
        }
    }

    /**
     * Write session to file
     *
     * @param dss
     */
    private void writeSession(Session dss) {

        try {
            // Open output streams
            FileOutputStream fos;
            fos = new FileOutputStream(new File(fullPathS));
            ObjectOutputStream oos;
            oos = new ObjectOutputStream(fos);

            // Write session object to file
            oos.writeObject(dss);

            // Close streams
            oos.close();
            fos.close();
        } catch (IOException e) {

            // Print error info and exit
            System.err.print(e.toString());
            System.exit(1);
        }
    }

    /**
     * Retrieve previous session
     *
     * @return
     */
    public Session getPrevSess() {

        try {
            // Open input streams
            FileInputStream fi;
            fi = new FileInputStream(new File(fullPathS));
            ObjectInputStream oi;
            oi = new ObjectInputStream(fi);

            // Read in session
            Session dss = (Session) oi.readObject();

            // Close streams
            oi.close();
            fi.close();

            // Return session
            return dss;

        } catch (IOException | ClassNotFoundException e) {

            // Print error info and exit
            System.err.print(e.toString());
            System.exit(1);
        }
        return null;
    }

    /**
     * Return true if a previous session file exists
     *
     * @return
     */
    public boolean doesPrevSessExist() {
        return (new File(fullPathS)).isFile();
    }
}
