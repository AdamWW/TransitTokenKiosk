import com.google.gson.JsonObject;
import com.ost.services.OSTAPIService;
import com.ost.services.v1_1.Manifest;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Transaction implements Serializable {

    private static final long serialVersionUID = 5288849340823875094L;
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
    private final String id;
    private final String fromId;
    private final String toId;
    private final int actionId;
    private final long timestampMillis;
    private final String transFee;
    private final String amount;

    private String toName;
    private String fromName;
    private String transType;

    public Transaction(JsonObject transData, Manifest services) {
        id = transData.get("id").getAsString();
        fromId = transData.get("from_user_id").getAsString();
        toId = transData.get("to_user_id").getAsString();
        actionId = transData.get("action_id").getAsInt();
        timestampMillis = transData.get("timestamp").getAsLong();
        transFee = transData.get("transaction_fee").getAsString();
        amount = transData.get("amount").getAsString();

        fromName = getUserName(fromId, services);
        toName = getUserName(toId, services);
        transType = determineTransType(toName, fromName, actionId);

    }

    private static String getUserName(String userId, Manifest services) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", userId);

        if(userId.equals(Main.COMP_ID)) {
            return "TransitToken";
        }

        try {
            JsonObject response = (services.users.get(params));
            JsonObject data = response.getAsJsonObject("data");
            JsonObject user = data.getAsJsonObject("user");
            return user.get("name").getAsString();
        } catch (IOException | OSTAPIService.MissingParameter e) {
            throw new RuntimeException("Exception attempting to retrieve user data.", e);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder(FORMAT.format(new Date(timestampMillis))).append(" | ").append(transType)
                .append(" | ").append(fromName).append(" | ").append(toName).append(" | ").append(amount).toString();
    }

    public String getId() {
        return id;
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }

    public int getActionId() {
        return actionId;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public String getTransFee() {
        return transFee;
    }

    public String getAmount() {
        return amount;
    }

    public String getToName() {
        return toName;
    }

    public String getFromName() {
        return fromName;
    }

    public String getTransType() {
        return transType;
    }

    private static String determineTransType(String toName, String fromName, int actionId) {
        String type = "Unknown";

        if (actionId == 39594) {
            type = "First Ride Free";
        } else if (toName.contains("Station")) {
            type = "Subway Fare";
        } else if (actionId == 39596 || actionId == 39595 || actionId == 39717) {
            type = "Customer Transfer";
        } else if (actionId == 39297) {
            type = "Loyalty Reward";
        } else if (fromName.equals("TransitToken")) {
            type = "Purchase";
        }

        return type;
    }
}
