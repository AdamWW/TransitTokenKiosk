import com.google.gson.JsonObject;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class User implements Serializable {

    private static final long serialVersionUID = -398710854627965281L;
    private final String name;
    private final String id;

    private float balance;
    private long totalRides = 0;
    private Date dateOfLastRide = new Date(Long.MIN_VALUE);;
    private int consecutiveDaysOfRides = 0;

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public float getBalance() {
        return balance;
    }

    public void setBalance(float balance) {
        this.balance = balance;
    }

    /**
     * This method resets the date of the user's last ride to 'now' and determines if they've earned any rewards
     *
     * @return number of TTO tokens they've earned in rewards
     */
    public int addNewRide() {
        int tokensEarned = 0;

        Date date = new Date();

        Calendar cal1 = new GregorianCalendar();
        Calendar cal2 = new GregorianCalendar();

        cal1.setTime(date);
        cal2.setTime(dateOfLastRide);

        long day1 = cal1.get(Calendar.DAY_OF_YEAR);
        long day2 = cal2.get(Calendar.DAY_OF_YEAR);

        long daysBetween = day2 - day1;


        if (daysBetween == 1) {
            consecutiveDaysOfRides++;
        } else {
            //user went more than a day between rides - zero out consecutive count
            consecutiveDaysOfRides = 0;
        }

        if (consecutiveDaysOfRides == 5) {
            //award the user 1 TTO for 5 consecutive days of riding, then zero it out
            tokensEarned += 1;
            consecutiveDaysOfRides = 0;
        }

        totalRides++;

        if (totalRides % 20 == 0) {
            //reward user for every 20th ride
            tokensEarned += 4;
        }

        this.dateOfLastRide = date;

        return tokensEarned;
    }

    public User(JsonObject userData) {

        JsonObject data = userData.getAsJsonObject("data");
        JsonObject user = data.getAsJsonObject("user");

        name = user.get("name").getAsString();
        id = user.get("id").getAsString();
        balance = user.get("token_balance").getAsFloat();
    }

    public String toString() {
        return new StringBuilder("Name: ").append(name)
                .append("\nID: ").append(id)
                .append("Balance: ").append(balance).toString();
    }

}
