import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class Driver {

    //Attributes
    String name;
    String city;
    String workload;

    // Global Variables
    public static int rowsAffected;

    //Method

    /**
     * The constructor for the Driver class. It is not used, but has been included for the sake of completeness.
     *
     * @param name The full name of the driver.
     * @param city The city that s/he covers.
     * @param workload The number of deliveries s/he to make.
     */
    public Driver(String name, String city, String workload) {
        this.name = name;
        this.city = city;
        this.workload = workload;
    }

    /**
     * The method to create a new record of a driver in the <code>driver</code> table. It is used as a utility method
     * in the <code>captureNewDriver</code> method in the main class of the program where it is invoked by the
     * <code>main</code> method.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>
     * @param input The Scanner instance from the <code>main</code> method needed here to read input from the user
     *              with the help of the utility class, <code>UserInput</code>
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void createDriver(Connection connection, Scanner input) throws SQLException {

        System.out.println("Capture New Driver\n");

        /*
         * User input of data needed for a new driver and setting the driverWorkload to 0 because that will initially
         * be the case.
         */
        String driverName = UserInput.readString("Full Name: ", input);
        String driverCity = UserInput.readString("Driver Location (City): ", input);
        int driverWorkload = 0;

        /* Use of PreparedStatement to insert these values into the driver table of the database.*/
        String mySQLQueryCreateDriver = "INSERT INTO driver VALUES (?, ?, ?, ?);";
        PreparedStatement pstmtCreateDriver = connection.prepareStatement(mySQLQueryCreateDriver);
        pstmtCreateDriver.setString(1, null); // Auto_increment
        pstmtCreateDriver.setString(2, driverName);
        pstmtCreateDriver.setString(3, driverCity);
        pstmtCreateDriver.setInt(4, driverWorkload);

        /*
         * Execution of the statement and determination of whether the insert was successful based on the return
         * value of the method
         */
        rowsAffected = pstmtCreateDriver.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("The driver was successfully added.");
        } else {
            System.out.println("The driver could not be added. Please review your input.");
        }

        /* Closing of resource to prevent resource leaking. */
        pstmtCreateDriver.close();

    }

    /**
     * This method finds a driver name from the input of a driver id. It is used in <code>displayOrder</code> and
     * <code>printInvoice</code> methods in the Order class as well as the <code>ordersAllocatedToDriver</code>
     * method in the main class of the program.
     *
     * @param connection The Connection resource from the main <code>method</code> needed here for
     *                   <code>PreparedStatement</code>
     * @param driverId The unique internal identification number for the driver.
     * @return The name of the driver corresponding to the entered driver unique id.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static String findDriverName(Connection connection, int driverId) throws SQLException {

        /*Declaration and initialisation of this variable because it is needed outside the while loop.*/
        String driverName = "";

        /* Use of PreparedStatement to select fields from driver for a specific driver ID */
        String mySQLQueryDriverName = "SELECT * FROM driver WHERE driver_id = ?;";
        PreparedStatement pstmtDriverName = connection.prepareStatement(mySQLQueryDriverName);
        pstmtDriverName.setInt(1, driverId);

        /*
         * Execution of the statement and storage of the return value in a ResultSet. The getter method of the
         * ResultSet is used to obtain the value of the driverName.
         */
        ResultSet resultsDriverName = pstmtDriverName.executeQuery();
        while (resultsDriverName.next()) {
            driverName = resultsDriverName.getString("driver_name");
        }

        /* Closing resources to prevent resource leaking.*/
        pstmtDriverName.close();
        resultsDriverName.close();

        return driverName;

    }

    /**
     * A method to find the driver unique id from the driver's full name. It is used in the
     * <code>ordersAllocatedToDriver</code> method in the main class of the program.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param input The Scanner instance from the <code>main</code> method needed to read input with the help of the
     *              utility class <code>UserInput</code>.
     * @return The driver id corresponding to the full name that was entered.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static int findDriverId(Connection connection, Scanner input) throws SQLException {

        /* Declaration and initialisation of this variable because it is needed outside the while loop*/
        int driverId = 0;

        /* User input of the driver name.*/
        String driverNamePrompt = "Please enter the driver's full name.";
        String driverName = UserInput.readString(driverNamePrompt, input);

        /* Use of PreparedStatement to select the driver_id field from the driver table for the driver name selected. */
        String mySQLQueryDriverId = "SELECT driver_id FROM driver WHERE LOWER(driver_name) = LOWER(?);";
        PreparedStatement pstmtDriverId = connection.prepareStatement(mySQLQueryDriverId);
        pstmtDriverId.setString(1, driverName);

        /* Execution of the statement and return of the method as a ResultSet.*/
        ResultSet resultsDriverId = pstmtDriverId.executeQuery();

        /* A try/catch block to catch situations where there is no matching driverId.*/
        try{
            while(resultsDriverId.next()) {
                driverId = resultsDriverId.getInt("driver_id");
            }
        } catch (SQLException e) {
            System.out.println("Driver.findDriverId SQL Exception. Unknown driver.");
        }

        /*Closing resources to prevent resource leaking.*/
        pstmtDriverId.close();
        resultsDriverId.close();

        return driverId;

    }

    /**
     * A method that allocates a driver id to an order based on the restaurant city and the driver's workload. It is
     * used in the <code>addInitialOrderDetails</code> method in the <code>Order</code> class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param city The restaurant location (city) needed for the decision-making process.
     * @return The unique driver if that should be allocated to the delivery.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static int driverAllocation(Connection connection, String city) throws SQLException {

        /* MySQL Query to find the highest driver workload for an area. */
        String mySQLHighestWorkload = "SELECT MAX(driver_workload) as max_workload FROM driver WHERE driver_city = ?";

        PreparedStatement pstmtHighestWorkload = connection.prepareStatement(mySQLHighestWorkload);
        pstmtHighestWorkload.setString(1, city);

        ResultSet resultsHighestWorkload = pstmtHighestWorkload.executeQuery();
        int maxWorkload = 0;
        while (resultsHighestWorkload.next()) {
            maxWorkload = resultsHighestWorkload.getInt("max_workload");
        }

        /* MySQL Query statement. */
        String mySQLQueryByCity = "SELECT * FROM driver WHERE driver_city = ?";

        /* Insertion of, city, variable into the MySQL statement.*/
        PreparedStatement pstmtByCity = connection.prepareStatement(mySQLQueryByCity);
        pstmtByCity.setString(1, city);

        /* Execution of the query and use of the ResultSet to get access to its getter methods. */
        ResultSet resultsByCity = pstmtByCity.executeQuery();

        /*Declaration and initialisation of the return variable so that it can be accessed outside the while loop. */
        int driverId = 0;

        /*
         * Logic for determination of workload comparison between drivers to ensure the driver with the lowest
         * workload gets allocated to the delivery.
         */
        while (resultsByCity.next()) {
            int driverWorkload = resultsByCity.getInt("driver_workload");
            if (driverWorkload <= maxWorkload) {
                driverId = resultsByCity.getInt("driver_id");
                maxWorkload = driverWorkload;
            }
        }

        /* Closing of resources to prevent resource leaking. */
        pstmtHighestWorkload.close();
        pstmtByCity.close();
        resultsHighestWorkload.close();
        resultsByCity.close();

        return driverId;

    }

    /**
     * When a driver is allocated a new order or when an order is finalised it is necessary to adjust the workload of
     * that particular driver. This method contains that functionality. It is used in the
     * <code>addInitialOrderDetails</code> and <code>makeFinal</code> methods in the <code>Order</code> class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for
     *                   <code>PreparedStatement</code>.
     * @param typeOfUpdate This determines whether the update is to remove or add an order to the driver's
     *                     <code>driver_workload</code> field in the <code>driver</code> table.
     * @param driverId The unique id number for the driver that needs to be updated.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void updateDriverWorkload(Connection connection, String typeOfUpdate, int driverId) throws SQLException {

        /* Declaration and initialisation of this variable because it is needed outside the while loop. */
        int currentWorkload = 0;

        /* Use of PreparedStatement to select the driver workload for a specific driver ID.*/
        String mySQLQueryCurrentWorkload = "SELECT driver_workload FROM driver WHERE driver_id = ?;";
        PreparedStatement pstmtCurrentWorkload = connection.prepareStatement(mySQLQueryCurrentWorkload);
        pstmtCurrentWorkload.setInt(1, driverId);

        /*
         * Execution of statement and return by the method of a ResultSet. The getter method of this ResultSet is
         * used to obtain the value of currentWorkload.
         */
        ResultSet resultsCurrentWorkload = pstmtCurrentWorkload.executeQuery();
        while (resultsCurrentWorkload.next()) {
            currentWorkload = resultsCurrentWorkload.getInt("driver_workload");
        }

        /*
         * An if/else block that uses the typeOfUpdate parameter to decide if the workload should be decreased or
         * increased by one.
         */
        if (typeOfUpdate.equalsIgnoreCase("add")) {
            currentWorkload += 1;
        } else if (typeOfUpdate.equalsIgnoreCase("remove")) {
            currentWorkload -= 1;
        }

        /* Use of PreparedStatement to update the specific driver's workload to the new value. */
        String mySQLQueryUpdateWorkload = "UPDATE driver SET driver_workload = ? WHERE driver_id = ?;";
        PreparedStatement pstmtUpdateWorkload = connection.prepareStatement(mySQLQueryUpdateWorkload);
        pstmtUpdateWorkload.setInt(1, currentWorkload);
        pstmtUpdateWorkload.setInt(2, driverId);

        /* Execution of statement and determination if the update was successful based on the return value of the
        method. */
        rowsAffected = pstmtUpdateWorkload.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("The driver's workload has successfully been updated.");
        } else {
            System.out.println("The update could not be completed. Please review your input.");
        }

        /* Closing of resources to prevent resource leaking. */
        pstmtUpdateWorkload.close();
        pstmtCurrentWorkload.close();
        resultsCurrentWorkload.close();

    }

    /**
     * This method covers the functionality of finding all orders that is allocated to a specific user. It is invoked
     * in the <code>ordersAllocatedToDriver</code> method in the main class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param driverId The unique driver if of the driver of interest.
     * @return An ArrayList with the order numbers of all orders allocated to the driver identified by the
     * <code>driverId</code>.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static ArrayList<Integer> findOrdersAllocatedToDriver(Connection connection, int driverId) throws SQLException {

        /*Declaration of an ArrayList to store the various order numbers in. */
        ArrayList<Integer> driversAllocated = new ArrayList<>();

        /*
         * Use of PreparedStatement to select all order number for a specific driver ID and that have a finalised
         * value of false in the orders table.
         */
        String mySQLQueryDriverAllocation = "SELECT order_number FROM orders WHERE driver_id = ? AND FINALISED IS " +
                "FALSE;";
        PreparedStatement pstmtDriverAllocation = connection.prepareStatement(mySQLQueryDriverAllocation);
        pstmtDriverAllocation.setInt(1, driverId);

        /*
         * Execution of statement and return of a ResultSet from the method. The getter method of the ResultSet is
         * used to obtain the list of order numbers.
         */
        ResultSet resultsDriverAllocation = pstmtDriverAllocation.executeQuery();
        while (resultsDriverAllocation.next()) {
            /* Adding each order number to the ArrayList. */
            driversAllocated.add(resultsDriverAllocation.getInt("order_number"));
        }

        /* Closing of resources to prevent resource leaking.*/
        pstmtDriverAllocation.close();
        resultsDriverAllocation.close();

        return driversAllocated;

    }

    /**
     * A generic method to handle updates of any fields in the <code>driver</code> table except for the
     * <code>driver_id</code> field which is unique.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param fieldToUpdate The specific field name of the field that should be updated.
     * @param newValue The new value of the field.
     * @param driverId The unique id number of the driver that needs to be updated.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void performFieldUpdate(Connection connection, String fieldToUpdate, String newValue, int driverId) throws SQLException {

        /*
         * Use of PreparedStatement to set up an update statement to update any of the fields in the driver table
         * except the driver_id which is fixed and AUTO_INCREMENT.
         */
        String mySQLQueryFieldUpdate =
                "UPDATE driver SET " + fieldToUpdate  + " = '" + newValue + "' WHERE driver_id = ?;";
        PreparedStatement pstmtFieldUpdate = connection.prepareStatement(mySQLQueryFieldUpdate);
        pstmtFieldUpdate.setInt(1, driverId);

        /*
         * Execution of statement and determination of whether the update was successful or not based on the return
         * value of the method.
         */
        rowsAffected = pstmtFieldUpdate.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("The driver was updated successfully.\n");
        } else {
            System.out.println("The driver update could not be done. Please check that the driverId is correct.\n");
        }

        /* Closing of resource to prevent resource leaking. */
        pstmtFieldUpdate.close();

    }
}