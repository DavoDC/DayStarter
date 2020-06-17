package main;

import java.awt.Component;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Models a session for a GUI program
 *
 * @author David
 */
public class Session implements Serializable {

    // Interface Components mapped to their name
    private final HashMap<String, Component> compMap;

    /**
     * Create session
     *
     * @param gui The interface (extended JFrame)
     * @param compNames The component names
     */
    public Session(GUI gui, String[] compNames) {

        // Initialize map
        compMap = new HashMap<>();

        // For each component name
        for (String curCompName : compNames) {

            // Get component from GUI
            Component curComp = gui.getComponentByName(curCompName);

            // Add (component name + component) pair to map
            compMap.put(curCompName, curComp);
        }
    }

    /**
     * Get the component with the given name OR null if not found
     *
     * @param name
     * @return
     */
    public Component getComp(String name) {

        // Retrieve component from map
        return compMap.get(name);
    }
}
