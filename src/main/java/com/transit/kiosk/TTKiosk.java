package com.transit.kiosk;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.ost.services.OSTAPIService;
import com.ost.services.v1_1.Manifest;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class TTKiosk extends JFrame implements WindowListener, WebcamViewerPanel.ImageFoundListener {
    private static final int FARE_AMNT = 4;
    private static final String USER_DATA_SERIALIZED_FILE_NAME = "TTO_USER_DATA.ser";
    private static final String LOYALTY_20_ACTION_ID = "39297";
    private static final String LOYALTY_5_ACTION_ID = "39050";
    private static final String FARE_ACTION_ID = "39717";
    private static final String STATION_TO_COMP_ACTION_ID = "39051";

    private final WebcamViewerPanel webcamViewerPanel;
    private final Manifest services;
    private final Map<String, User> userMap = new HashMap<>();
    private final GridBagConstraints gbc = new GridBagConstraints();
    private final JPanel viewerPanel = new JPanel(new BorderLayout());
    private final JPanel userInfoPanel = new JPanel();
    private final Image checkmark;


    public TTKiosk(Manifest services) throws HeadlessException, IOException {
        //try to deserialize first...
        deserializeUserData();

        setIconImage(ImageIO.read(Main.class.getResource("/images/tt_icon.jpg")));
        this.services = services;
        checkmark = ImageIO.read(Main.class.getResource("/images/check.png"));
        setTitle("TransitToken Kiosk - " + Main.stationName);
        userInfoPanel.setLayout(new BoxLayout(userInfoPanel, BoxLayout.PAGE_AXIS));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0.5;
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;

        webcamViewerPanel = new WebcamViewerPanel(this);
        webcamViewerPanel.setSize(new Dimension(200, 200));
        viewerPanel.add(webcamViewerPanel, BorderLayout.CENTER);

        addWindowListener(this);

        add(viewerPanel, gbc);

        SwingUtilities.invokeLater(webcamViewerPanel);

        pack();
        setLocationRelativeTo(null);
        setSize(new Dimension(500, 500));
        setVisible(true);

    }

    //WindowListener Methods:
    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
        webcamViewerPanel.close();
    }

    @Override
    public void windowClosing(WindowEvent e) {
        serializeUserData();
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        System.out.println("webcam viewer resumed");
        webcamViewerPanel.resume();
    }

    @Override
    public void windowIconified(WindowEvent e) {
        System.out.println("webcam viewer paused");
        webcamViewerPanel.pause();
    }

    private void populateUserInfoPanel(User user) throws IOException, OSTAPIService.MissingParameter {
        Font f = new Font("Courier", Font.BOLD, 18);

        JLabel nameLabel = new JLabel("User Name: " + user.getName());
        nameLabel.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        JLabel idLabel = new JLabel("User ID: " + user.getId());
        idLabel.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        JLabel balanceLabel = new JLabel("User Balance: " + user.getBalance() + " TTO");
        balanceLabel.setFont(f.deriveFont(f.getStyle() | Font.BOLD));

        Component trans = createTransScrollPane(user);

        userInfoPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED), BorderFactory.createTitledBorder("User Info")));
        userInfoPanel.removeAll();
        userInfoPanel.add(nameLabel);
        userInfoPanel.add(idLabel);
        userInfoPanel.add(balanceLabel);
        userInfoPanel.add(new JLabel()); //spacer
        userInfoPanel.add(trans);
        gbc.gridx = 1;
        gbc.gridy = 0;
        add(userInfoPanel, gbc);
    }

    private Component createTransScrollPane(User user) throws IOException, OSTAPIService.MissingParameter {
        JScrollPane scrollPane = new JScrollPane();

        //get the user's most recent transactions
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", user.getId());
        params.put("page_no", 1);
        params.put("limit", 50);
        JsonObject response = services.ledger.get(params);

        //table stuff//
        JTable table;
        Vector<Vector<String>> tableData = new Vector<>();

        if (response.get("success").getAsBoolean()) {
            JsonArray trans = response.get("data").getAsJsonObject().get("transactions").getAsJsonArray();
            System.out.println("Transactions: " + trans);

            trans.forEach(jsonElement -> {
                if(jsonElement.getAsJsonObject().get("status").getAsString().equals("complete")) {
                    Transaction t = new Transaction(jsonElement.getAsJsonObject(), services);
                    tableData.add(t.toRowVector());
                }
            });
        }

        table = new JTable(tableData, Transaction.COL_NAMES);

        scrollPane.setViewportView(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Recent Transactions:"));

        return scrollPane;
    }

    private void farePaid(User user) throws IOException, OSTAPIService.MissingParameter {
        String path = "https://api.qrserver.com/v1/create-qr-code/?data=" + user.getId() + "&;size=200x200";
        URL url = new URL(path);
        BufferedImage image = ImageIO.read(url);
        JLabel label = new JLabel(new ImageIcon(image)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(checkmark, 20, 20, null);
            }

        };
        viewerPanel.removeAll();
        viewerPanel.add(label, BorderLayout.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        setLayout(new GridBagLayout());
        add(viewerPanel, gbc);
        populateUserInfoPanel(user);
        viewerPanel.validate();
        pack();
    }

    private void handleUserFare(User user) throws IOException, InterruptedException, OSTAPIService.MissingParameter {
        //First, we need to deduct the user's fare from their balance, provided they have enough
        if (user.getBalance() >= FARE_AMNT) {
            //the user can afford the ride... do the transaction
            HashMap<String, Object> params = new HashMap<>();
            params.put("from_user_id", user.getId());
            params.put("to_user_id", Main.selectedStationId);
            params.put("action_id", FARE_ACTION_ID);
            JsonObject response = services.transactions.execute(params);
            if (response.get("success").getAsBoolean()) {
                //fare successfully paid
                farePaid(user);

                //send the tokens from the station back to the company

                params.put("from_user_id", Main.selectedStationId);
                params.put("to_user_id", Main.COMP_ID);
                params.put("action_id", STATION_TO_COMP_ACTION_ID);
                response = services.transactions.execute(params);

                if(!response.get("success").getAsBoolean()) {
                    JOptionPane.showMessageDialog(this, "Sorry, there was a problem executing the transactions. Please see your transaction log on our webstie www.TransitToken.org", "com.transit.kiosk.Transaction ERROR", JOptionPane.WARNING_MESSAGE);
                }
            }

            int tokensEarned = user.addNewRide();

            if (tokensEarned > 0) {
                //if they've earned any tokens, send them.
                //need to separate into 3 possible categories: 4 TTO == 20 ride reward, 1 TTO == 5 consecutive days riding, 5 TTO == both

                params.put("from_user_id", Main.COMP_ID);
                params.put("to_user_id", user.getId());
                response = services.transactions.execute(params);

                if (tokensEarned == 6) {
                    //send both 20 rides reward and 5 consecutive days reward
                    params.put("action_id", LOYALTY_20_ACTION_ID);
                    services.transactions.execute(params);

                    params.put("action_id", LOYALTY_5_ACTION_ID);
                    response = services.transactions.execute(params);

                    //show popup
                    showAlert("Thank you for your loyalty! For your 20th ride AND riding 5 consecutive days, here's a Token of our appreciation!\n\n+6 TTO");
                } else if (tokensEarned == 4) {
                    //send 20 rides reward
                    params.put("action_id", LOYALTY_20_ACTION_ID);
                    response = services.transactions.execute(params);

                    //show popup
                    showAlert("Thank you for your loyalty! For your 20th ride, here's a Token of our appreciation!\n\n+4 TTO");
                } else if (tokensEarned == 2) {
                    //send 5 consecutive days reward
                    params.put("action_id", LOYALTY_5_ACTION_ID);
                    response = services.transactions.execute(params);

                    //show popup
                    showAlert("Thank you for your loyalty! For riding 5 consecutive days, here's a Token of our appreciation!\n\n+2 TTO");
                } else {
                    //somehow got an incorrect amount, do nothing
                }
            }

            if (!response.get("success").getAsBoolean()) {
                //if the transaction failed, alert the user
                JOptionPane.showMessageDialog(this, "Sorry, there was a problem executing the transactions. Please see your transaction log on our webstie www.TransitToken.org", "com.transit.kiosk.Transaction ERROR", JOptionPane.WARNING_MESSAGE);
            }

        } else { //display message to user notifying them their balance is too low
            JOptionPane.showMessageDialog(this, "Sorry, your balance is too low. Please reload your balance at our website www.TranitToken.org", "Balance Alert!", JOptionPane.WARNING_MESSAGE);
        }

        //need to update user's balance too...
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", user.getId());
        JsonObject response = services.balances.get(params);
        float bal = response.get("data").getAsJsonObject().get("balance").getAsJsonObject().get("available_balance").getAsFloat();
        user.setBalance(bal);

        //resume the webcam video feed and reset fields for the next rider
        Thread.sleep(10000); //ten second pause

        userInfoPanel.removeAll();
        remove(userInfoPanel);
        viewerPanel.removeAll();
        viewerPanel.add(webcamViewerPanel, BorderLayout.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        setLayout(new GridBagLayout());
        add(viewerPanel, gbc);
        viewerPanel.validate();
        pack();
        setSize(new Dimension(500, 500));
    }

    private void showAlert(String alertMsg) {
        JOptionPane pane = new JOptionPane(alertMsg, JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog(this, "!");
        dialog.setModal(false);
        dialog.setVisible(true);

        new Timer(3000, e -> dialog.setVisible(false)).start();
    }

    @Override
    public void imageFound(Image image) {
        try {
            webcamViewerPanel.pause();
            String result = decodeQRCode(image);

            if (result != null) {
                System.out.println("Decoded code: " + result);
                //we found a code - determine if it's a valid user id
                Map<String, Object> params = new HashMap<>();
                params.put("id", result);

                JsonObject response = (services.users.get(params));

                if (response.get("success").getAsBoolean()) {
                    java.awt.Toolkit.getDefaultToolkit().beep(); //Beep!

                    User user;
                    if (userMap.containsKey(result)) { //result, if the OST services api call is a success, equates to the user's ID
                        user = userMap.get(result);
                        //make sure the user data is up-to-date
                        user.setBalance(response.getAsJsonObject("data").getAsJsonObject("user").get("token_balance").getAsFloat());

                    } else { //otherwise, this user isn't in our mappings, create a new user
                        user = new User(response);
                        userMap.put(result, user); //make sure we keep track of the new user
                    }

                    handleUserFare(user);

                }
            }

        } catch (InterruptedException | IOException | OSTAPIService.MissingParameter e) {
            throw new RuntimeException("Exception parsing QR code or otherwise submitting transactions.", e);
        }
        webcamViewerPanel.resume();

    }

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }


    private static String decodeQRCode(Image qrCodeimage) throws IOException {
        LuminanceSource source = new BufferedImageLuminanceSource(toBufferedImage(qrCodeimage));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        try {
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    private void serializeUserData() {
        try {
            FileOutputStream fos = new FileOutputStream(USER_DATA_SERIALIZED_FILE_NAME);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(userMap);
            oos.close();
            fos.close();
            System.out.println("Serialized HashMap data is saved in " + USER_DATA_SERIALIZED_FILE_NAME);
        } catch (IOException ioe) {
            throw new RuntimeException("Exception in trying to serialize user data.", ioe);
        }
    }

    private void deserializeUserData() {
        File f = new File(USER_DATA_SERIALIZED_FILE_NAME);
        if (f.exists() && !f.isDirectory()) {

            try {
                HashMap<String, User> map;
                FileInputStream fis = new FileInputStream(USER_DATA_SERIALIZED_FILE_NAME);
                ObjectInputStream ois = new ObjectInputStream(fis);
                map = (HashMap) ois.readObject();
                userMap.putAll(map);
                ois.close();
                fis.close();
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException("Exception when attempting to de-serialize user data.", e);
            }
        }

    }
}
