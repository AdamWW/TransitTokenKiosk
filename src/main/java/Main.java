import com.ost.OSTSDK;
import com.ost.services.v1_1.Manifest;

import javax.swing.*;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Main {
    public static final String COMP_ID = "40b637e9-71df-4294-93be-87bc7c54c46b";

    private static final Map<String, String> stationMap = new HashMap<>();

    private static OSTSDK ostObj;
    private static Manifest services;

    public static String selectedStationId = "";
    public static String stationName = "";

    static {
        stationMap.put("Park Station", "9fc767bf-decd-43bf-a816-6814af75e1e0");
        stationMap.put("Coach Station", "c3da5c97-e9b4-4eed-91ab-2de854f40a53");
        stationMap.put("Main Station", "fe31d6c9-eacb-4c5b-98ff-57e86a4bbb31");
        stationMap.put("South Station", "d59c28b0-0d64-452a-a121-41a69be0262b");
        stationMap.put("Gallery Station", "d168e315-518c-4cc5-954c-8baffe0f2ac3");
        stationMap.put("Bumper Station", "16468163-c74a-462c-9ea5-86151ca18c5e");
        stationMap.put("Chip Station", "c067f30d-41c0-474b-84f0-a4cf1364d3b2");
        stationMap.put("Arena Station", "65dbad49-9c64-4a22-974f-7cfd3bd7a85b");
        stationMap.put("Spade Station", "06fc5eb2-567d-48d2-93d1-748358dbd58e");
        stationMap.put("Rolling Station", "9c438db4-623b-4284-9923-a923b8ea7478");
        stationMap.put("Juniper Station", "7df144c8-f333-4ed6-9126-33cf95fcfe7c");
        stationMap.put("Pineapple Station", "cd1da639-2dcf-4647-9af9-e0bfe692d7d9");
        stationMap.put("North Station", "7d965e23-74a4-467b-a6c8-b89ecc3e209a");
        stationMap.put("Market Station", "9113c2f6-9e34-4f68-b53d-ebed2fb9ba88");
        stationMap.put("Sky Station", "718487a1-a5ca-4abe-8a59-2135b4dad99f");
        stationMap.put("Uptown Station", "e09a5d33-3025-4ebc-b904-30c5e2a09607");
        stationMap.put("Airport Station", "e4fc12e2-11b7-4fd6-9eff-d6329db9a3ca");
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

        //show startup popup to select terminal/stop/kiosk location
        Icon icon = new ImageIcon(Main.class.getResource("/images/tt_icon.jpg"));
        Vector<String> stationList = new Vector<>(stationMap.keySet());
        Collections.sort(stationList);
        JComboBox<String> stations = new JComboBox<>(stationList);
        stations.addActionListener(e -> {
            selectedStationId = stationMap.get(stations.getSelectedItem());
            stationName = stations.getSelectedItem().toString();
        });
        stations.setSelectedIndex(0);
        int okcancel = JOptionPane.showConfirmDialog(null, stations,
                "Station Selection", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, icon);
        if(okcancel == JOptionPane.OK_OPTION) {
            initializeOSTServices();
            new TTKiosk(services);
        } else {
            System.exit(0);
        }
    }

    private static void initializeOSTServices() {
        HashMap<String, Object> sdkConfig = new HashMap<>();
        sdkConfig.put("apiEndpoint", "https://sandboxapi.ost.com/v1.1");
        sdkConfig.put("apiKey", "60b5fb01f5a4639d894d"); // replace with the API Key you obtained earlier
        sdkConfig.put("apiSecret", "bdd11de00a0c3c870b7c269500d0aa2d939cbe8d4092871f4f1c6c1ce4d6bb76"); // replace with the API Secret you obtained earlier

        // Initialize SDK object
        ostObj = new OSTSDK(sdkConfig);
        services = (Manifest) ostObj.services;

//        JsonObject response = (services.users.list(new HashMap<>()));
//
//        System.out.println(ReflectionToStringBuilder.toString(response));
    }
}
