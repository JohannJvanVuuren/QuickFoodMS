import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Restaurant {

    //Attributes
    String name;
    String phoneNumber;
    String city;

    // Global Variables
    public static int rowsAffected;

    //Methods

    /**
     * The constructor for the Restaurant class. It is not used but has been included for the sake of completeness.
     *
     * @param name The name of the restaurant.
     * @param phoneNumber The contact number of the restaurant.
     * @param city The city in which the restaurant is located. This is important because it determines, together
     *             with workload, which driver will be allocated to the delivery.
     */
    public Restaurant(String name, String phoneNumber, String city) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.city = city;
    }

    /**
     * This method handles the creation of new restaurants to add to the database. It is invoked in the
     * <code>captureNewRestaurant</code> method in the main class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param input The Scanner instance from the <code>main</code> method needed here for the reading of user input
     *              with the help of the utility class <code>UserInput</code>.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void createNewRestaurant(Connection connection, Scanner input) throws SQLException {

        System.out.println("""
                Capture New Restaurant
                ----------------------
                """);

        /*User input of the details needed to register a new restaurant.*/
        String restaurantName = UserInput.readString("Restaurant Name: ", input);
        String restaurantPhoneNumber = UserInput.readString("Restaurant Phone Number: ", input);
        String restaurantCity = UserInput.readString("Restaurant Location (City): ", input);

        /* Use of PreparedStatement to set up a MySQL query statement. */
        String mySQLQueryCreateRestaurant = "INSERT INTO restaurant VALUES (?, ?, ?, ?)";
        PreparedStatement pstmtCreateRestaurant = connection.prepareStatement(mySQLQueryCreateRestaurant);
        pstmtCreateRestaurant.setString(1, null);
        pstmtCreateRestaurant.setString(2, restaurantName);
        pstmtCreateRestaurant.setString(3, restaurantPhoneNumber);
        pstmtCreateRestaurant.setString(4, restaurantCity);

        /*
         * The execution of the statement and determination of whether the insertion was successful or not based on
         * the return value of the method.
         */
        rowsAffected = pstmtCreateRestaurant.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("The restaurant was successfully created.\n");
        } else {
            System.out.println("The restaurant could not be created. Please review your input\n");
        }

        /* Closing resource to prevent resource leaking. */
        pstmtCreateRestaurant.close();

    }

    /**
     * A method to find the restaurant's ID from the first name of the restaurant entered by the user. This method is
     * used in the <code>openOrder</code> method in the <code>Order</code> class.
     *
     * @param connection The Connection resource from the <code>main</code> method of the program, needed for the
     *                   PreparedStatements.
     * @return The restaurant's ID as an integer.
     * @throws SQLException If underlying MySQL service fails.
     */
    public static int findRestaurantId(Connection connection, String restaurantName) throws SQLException {

        /* Declaration and initialisation of this variable because it is needed outside the while loop.*/
        int restaurantId = 0;

        /*
         * Use of PreparedStatement to set up a MySQL query statement to find a restaurant ID from the restaurantName
         * parameter of the method.
         */
        String mySQLQueryRestaurantId =
                "SELECT restaurant_id  FROM restaurant  WHERE LOWER(restaurant_name) = LOWER(?);";
        PreparedStatement pstmtRestaurantId = connection.prepareStatement(mySQLQueryRestaurantId);
        pstmtRestaurantId.setString(1, restaurantName);

        /*
         * Execution of the query and saving the result in a ResultSet. The getter method of the ResultSet is used to
         * obtain the restaurantId.
         */
        ResultSet resultsRestaurantId = pstmtRestaurantId.executeQuery();
        while (resultsRestaurantId.next()) {
            restaurantId = resultsRestaurantId.getInt("restaurant_id");
        }

        /*Closing of these resources to prevent resource leaking. */
        pstmtRestaurantId.close();
        resultsRestaurantId.close();

        return restaurantId;

    }

    /**
     * This method finds the restaurant name from the restaurant id entered by the user. It is invoked in the
     * <code>displayOrder</code> and <code>printInvoice</code> methods in the <code>Order</code> class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param restaurantId The unique id number of the restaurant.
     * @return The name of the restaurant as a String.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static String findRestaurantName(Connection connection, int restaurantId) throws SQLException {

        /* Declaration and initialisation of this variable because it is needed outside the while loop.*/
        String restaurantName = "";

        /*
         * Use of PreparedStatement to set up a MySQL query statement to find the restaurant name based on the
         * restaurantId parameter of the method.
         */
        String mySQLQueryRestaurantName = "SELECT restaurant_name FROM restaurant WHERE restaurant_id = ?;";
        PreparedStatement pstmtRestaurantId = connection.prepareStatement(mySQLQueryRestaurantName);
        pstmtRestaurantId.setInt(1, restaurantId);

        /*
         * Execution of the statement and return from the method as a ResultSet. The getter method of the ResultSet
         * is used to obtain the restaurantName variable.
         */
        ResultSet resultsRestaurantId = pstmtRestaurantId.executeQuery();
        while (resultsRestaurantId.next()) {
            restaurantName = resultsRestaurantId.getString("restaurant_name");
        }

        /* Closing of these resources to prevent resource leaking.*/
        pstmtRestaurantId.close();
        resultsRestaurantId.close();

        return restaurantName;

    }

    /**
     * This method finds the restaurant location from the restaurant name. It is invoked in the
     * <code>addInitialOrderDetails</code> and <code>printInvoice</code> methods in the <code>Order</code> class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param restaurantName The restaurant name passed in as an argument when the method is invoked.
     * @return The location of the restaurant as a String.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static String findRestaurantLocation(Connection connection, String restaurantName) throws SQLException{

        /* Declaration and initialisation of this variable because it is needed outside the while loop. */
        String restaurantLocation = "";

        /*
         * Use of PreparedStatement to set up a MySQL query statement to find the restaurant city from the
         * restaurantName parameter of the method.
         */
        String mySQLQueryRestaurantLocation = "SELECT restaurant_city FROM restaurant WHERE LOWER(restaurant_name) = LOWER(?); ";
        PreparedStatement pstmtRestaurantLocation = connection.prepareStatement(mySQLQueryRestaurantLocation);
        pstmtRestaurantLocation.setString(1, restaurantName);

        /*
         * Execution of the statement with a returned ResultSet. The getter method of the ResultSet is used to obtain
         * the restaurantLocation variable.
         */
        ResultSet resultsRestaurantLocation = pstmtRestaurantLocation.executeQuery();
        while (resultsRestaurantLocation.next()) {
            restaurantLocation = resultsRestaurantLocation.getString("restaurant_city");
        }

        /* Closing of resources to prevent resource leaking. */
        pstmtRestaurantLocation.close();
        resultsRestaurantLocation.close();

        return restaurantLocation;

    }

    /**
     * A method to find the restaurant's phone number from its unique id number. It is invoked in
     * <code>printInvoice</code> in the <code>Order</code> class.
     * @param connection The Connection from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>
     * @param restaurantId The unique restaurant id number passed in as an argument.
     * @return The restaurant phone number as a String.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static String findRestaurantPhoneNumber(Connection connection, int restaurantId) throws SQLException{

        /* Declaration and initialisation of this variable because it is needed outside the while loop. */
        String restaurantPhoneNumber = "";

        /*
         * Use of PreparedStatement to set up a MySQL query statement to find the restaurant's phone number with the
         * restaurantId parameter of the method.
         */
        String mySQLQueryRestaurantPhone = "SELECT restaurant_phone_num FROM restaurant WHERE restaurant_id = ?;";
        PreparedStatement pstmtRestaurantPhone = connection.prepareStatement(mySQLQueryRestaurantPhone);
        pstmtRestaurantPhone.setInt(1, restaurantId);

        /*
         * Execution of the statement with the return of a ResultSet. The getter method of ResultSet is used to
         * obtain the restaurantPhoneNumber variable.
         */
        ResultSet resultsRestaurantPhone = pstmtRestaurantPhone.executeQuery();
        while (resultsRestaurantPhone.next()) {
            restaurantPhoneNumber = resultsRestaurantPhone.getString("restaurant_phone_num");
        }

        /* Closing of resources to prevent resource leaking. */
        pstmtRestaurantPhone.close();
        resultsRestaurantPhone.close();

        return restaurantPhoneNumber;
    }

    /**
     * A generic method to update any field in the <code>restaurant</code> table except for the
     * <code>restaurant_id</code> field which is fixed and unique. It is invoked in the <code>updateRestaurant</code>
     * method in the main class.
     *
     * @param connection The Connection resource from the <code>main</code> menu needed here for the
     *                   <code>PreparedStatement</code>.
     * @param fieldToUpdate The database name of the field to be updated passed in as an argument.
     * @param newValue The new value of the field. Passed in as an argument.
     * @param restaurantId The unique restaurant id number passed in as an argument.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void performFieldUpdate(Connection connection, String fieldToUpdate, String newValue,
                                          int restaurantId) throws SQLException {

        /*
         * Use of PreparedStatement to set up a MySQL query statement to update any restaurant field as determined by
         * the parameters of this method.
         */
        String mySQLQueryFieldUpdate =
                "UPDATE restaurant SET " + fieldToUpdate  + " = '" + newValue + "' WHERE restaurant_id = ?;";
        PreparedStatement pstmtFieldUpdate = connection.prepareStatement(mySQLQueryFieldUpdate);
        pstmtFieldUpdate.setInt(1, restaurantId);

        /*
         * Execution of the statement and determination if the update was successful or not by means of the return
         * value of the method.
         */
        rowsAffected = pstmtFieldUpdate.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("The update was successful.\n");
        } else {
            System.out.println("The update could not be done. Please check that the customer ID is correct.\n");
        }

        /* Closing of resource to prevent resource leaking. */
        pstmtFieldUpdate.close();

    }

}