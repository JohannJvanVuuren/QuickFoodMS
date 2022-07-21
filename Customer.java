import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class Customer {

    //Attributes
    String firstName;
    String surname;
    String phoneNumber;
    String address;
    String city;
    String email;

    //Global variable
    public static int rowsAffected;

    //Methods

    /**
     * The constructor method for the Customer class. It is not used but included for the sake of completion.
     *
     * @param firstName   The first name of the customer.
     * @param surname     The surname of the customer.
     * @param phoneNumber The phone number of the customer.
     * @param address     The physical address of the customer for deliveries.
     * @param city        The city in which the customer lives.
     * @param email       The customer's email address.
     */
    public Customer(String firstName, String surname, String phoneNumber, String address, String city, String email) {
        this.firstName = firstName;
        this.surname = surname;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.city = city;
        this.email = email;
    }

    /**
     * A method to find the customer's ID from the first name and surname entered by the user. This method is used by
     * the <code>captureNewOrder</code> method in the <code>openOrder</code> method in the <code>Order</code> class.
     *
     * @param connection The Connection resource from the <code>main</code> method of the program, needed for the
     *                   PreparedStatements.
     * @return The customer's ID as an integer.
     * @throws SQLException If underlying MySQL service fails.
     */
    public static int findCustomerId(Connection connection, String firstName, String surname) throws SQLException {

        int customerId = 0;

        /* Use of PreparedStatement to set up a MySQL query statement to find the customer ID from the customer's
        firstName and surname parameters of this method.*/
        String mySQLQueryCustomerId =
                "SELECT customer_id FROM customer  WHERE LOWER(customer_firstname) = LOWER(?) AND " +
                        "LOWER(customer_surname) = LOWER(?);";
        PreparedStatement pstmtCustomerId = connection.prepareStatement(mySQLQueryCustomerId);
        pstmtCustomerId.setString(1, firstName);
        pstmtCustomerId.setString(2, surname);

        /*
        * Execution of the statement and return as a ResultSet. The getter method of the ResultSet is then used to
        * obtain the customerId variable if it is found. The try/catch block handles cases where no customerId is found.
        */
        ResultSet resultsCustomerId = pstmtCustomerId.executeQuery();

        try {
            while (resultsCustomerId.next()) {
                customerId = resultsCustomerId.getInt("customer_id");
            }
        } catch (SQLException e) {
            System.out.println("""
                Customer.findCustomerId ERROR:
                Search produced no result. Please review your input
                making sure the customer's firstname and surname is
                correct.
                """);
        }

        /* Closing of these resources to prevent a resource leak.*/
        pstmtCustomerId.close();
        resultsCustomerId.close();

        return customerId;

    }

    /**
     * A method to find the customer full name from the unique customer id. It is invoked in the
     * <code>displayOrder</code> and <code>printInvoice</code> methods in the <code>Order</code> class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param customerId The unique customer id number passed in as an argument.
     * @return The full name of the customer (firstname and surname concatenated).
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static String findCustomerName(Connection connection, int customerId) throws SQLException {

        /* Declaration and initialisation of these two variables here because they are needed outside the while loop. */
        String customerFirstname = "";
        String customerSurname = "";

        /*
         * Use of PreparedStatement to set up a MySQL query statement to find the customer's firstname and surname
         * based on the customerId parameter of this method.
         */
        String mySQLQueryCustomerName = "SELECT customer_firstname, customer_surname FROM customer WHERE customer_id = ?;";
        PreparedStatement pstmtCustomerName = connection.prepareStatement(mySQLQueryCustomerName);
        pstmtCustomerName.setInt(1, customerId);

        /*
         * Execution of the statement and return as a ResultSet. The getter method of the ResultSet is then used to
         * obtain the customerFirstName and customerSurname variables.
         */
        ResultSet resultsCustomerName = pstmtCustomerName.executeQuery();
        while (resultsCustomerName.next()) {
            customerFirstname = resultsCustomerName.getString("customer_firstname");
            customerSurname = resultsCustomerName.getString("customer_surname");
        }

        /* Closing of resources to prevent resource leaking. */
        pstmtCustomerName.close();
        resultsCustomerName.close();

        /* The firstname and surname is concatenated so that a full name is returned from this method. */
        return customerFirstname + " " + customerSurname;

    }

    /**
     * This method adds new customers to the database with the input from the user. The method
     * <code>captureNewOrder</code> in the main method checks if an order is for an existing or new customer and
     * invokes this method if it is a new customer.
     *
     * @param connection The Connection resource from the main method, needed for the PreparedStatement.
     * @param input      The Scanner instance from the main method that is needed for the methods in the UserInput
     *                   utility class.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void createNewCustomer(Connection connection, Scanner input, String firstName, String surname) throws SQLException {

        /* Reading customer demographics via the UserInput class and user input. */
        String customerPhoneNumber = UserInput.readString("Customer Phone Number (no spaces or punctuation): ", input);
        String customerAddress = UserInput.readString("Customer Address: ", input);
        String customerCity = UserInput.readString("Customer City: ", input);
        String customerEmail = UserInput.readString("Customer Email: ", input);

        /* Use of PreparedStatement to set up a MySQL query statement to insert new records in the customer table.*/
        String mySQLQuery = "INSERT INTO customer VALUES (?, ?, ?, ?, ?, ?, ?);";
        PreparedStatement pstmtCreateNewCustomer = connection.prepareStatement(mySQLQuery);
        pstmtCreateNewCustomer.setString(1, null);
        pstmtCreateNewCustomer.setString(2, firstName);
        pstmtCreateNewCustomer.setString(3, surname);
        pstmtCreateNewCustomer.setString(4, customerPhoneNumber);
        pstmtCreateNewCustomer.setString(5, customerAddress);
        pstmtCreateNewCustomer.setString(6, customerCity);
        pstmtCreateNewCustomer.setString(7, customerEmail);

        /*
         * The execution of the statement and determination of whether the insertion of the new record was successful
         * or not
         */
        rowsAffected = pstmtCreateNewCustomer.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("The customer was successfully added to the database.\n");
        } else {
            System.out.println("The customer could not be added. Please review your input.\n");
        }

        /* Closing of resource to prevent resource leaking.*/
        pstmtCreateNewCustomer.close();

    }

    /**
     * This method checks for any incomplete customer info by looking for <code>NULL</code> values. It is invoked in
     * <code>listOrdersWithIncompleteInfo</code> in the main class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @return An ArrayList with the unique customer ID's that have incomplete information.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static ArrayList<Integer> checkForIncompleteCustomerInfo(Connection connection) throws SQLException {

        /*
         * Declaration of an ArrayList because there could be more than one customer with incomplete information. With
         * this list the relevant customer ID can be added to a list.
         */
        ArrayList<Integer> incompleteCustomerInfo = new ArrayList<>();

        /*
         * Use of a PreparedStatement to set up a MySql query statement that will find records in the customer table
         * where any of the fields are NULL.
         */
        String mySQLQueryCustomerInfo = "SELECT * FROM customer WHERE customer_firstName IS NULL OR customer_surname " +
                "IS NULL OR customer_phone_num IS NULL OR customer_address IS NULL OR customer_city IS NULL OR " +
                "customer_email IS NULL;";
        PreparedStatement pstmtCustomerInfo = connection.prepareStatement(mySQLQueryCustomerInfo);

        /*
         * The execution of the statement and the return of a ResultSet from the method. The getter method from the
         * ResultSet will return customer_ ID(s) that have incomplete information. Each one of these are then added
         * to the ArrayList incompleteCustomerInfo. */
        ResultSet resultsCustomerInfo = pstmtCustomerInfo.executeQuery();
        while (resultsCustomerInfo.next()) {
            incompleteCustomerInfo.add(resultsCustomerInfo.getInt("customer_id"));
        }

        /* Closing resources to prevent resource leaking. */
        pstmtCustomerInfo.close();
        resultsCustomerInfo.close();

        /* Return of the ArrayList with customer ID's where there are incomplete fields.*/
        return incompleteCustomerInfo;

    }

    /**
     * A method to display customer details. It is invoked in the <code>listOrdersWithIncompleteInfo</code> method in
     * the main class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param customerId The unique customer ID passed in as an argument.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void displayCustomer(Connection connection, int customerId) throws SQLException{

        /* Declaration and initialisation of these variables because they are needed outside the while loop. */
        String customerSurname = "";
        String customerFirstname = "";
        String customerPhoneNum = "";
        String customerAddress = "";
        String customerCity = "";
        String customerEmail = "";

        /*
         * Use of PreparedStatement to set up a MySQL query statement to find all fields of a record from the
         * customer table identified by the customerId parameter of this method.
         */
        String mySQLQueryCustomerDetails = "SELECT * FROM customer WHERE customer_id = ?;";
        PreparedStatement pstmtCustomerDetails = connection.prepareStatement(mySQLQueryCustomerDetails);
        pstmtCustomerDetails.setInt(1, customerId);

        /*
         * Execution of the statement and return of a ResultSet from the method. The getter method from this
         * ResultSet is used to obtain the necessary information fields.
         */
        ResultSet resultsCustomerDetails = pstmtCustomerDetails.executeQuery();
        while (resultsCustomerDetails.next()) {
            customerFirstname = resultsCustomerDetails.getString("customer_firstname");
            customerSurname = resultsCustomerDetails.getString("customer_surname");
            customerPhoneNum = resultsCustomerDetails.getString("customer_phone_num");
            customerAddress = resultsCustomerDetails.getString("customer_address");
            customerCity = resultsCustomerDetails.getString("customer_city");
            customerEmail = resultsCustomerDetails.getString("customer_email");
        }

        /* Display of the customer's information.*/
        System.out.println("Customer Details.\n");
        System.out.println("Customer First Name: " + customerFirstname);
        System.out.println("Customer Surname: " + customerSurname);
        System.out.println("Customer Phone Number: " + customerPhoneNum);
        System.out.println("Customer Address: " + customerAddress);
        System.out.println("Customer City: " + customerCity);
        System.out.println("Customer Email: " + customerEmail);

        /* Closing of resources to prevent resource leaking. */
        pstmtCustomerDetails.close();
        resultsCustomerDetails.close();

    }

    /**
     * A generic method to update any field of the <code>customer</code> table except for the
     * <code>customer_id</code> field which is fixed and unique.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param fieldToUpdate The field name of the field that needs to be updated. Passed in as an argument.
     * @param newValue The new value of the field. Passed in as an argument.
     * @param customerId The unique customer ID number also passed in as an argument.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void performFieldUpdate(Connection connection, String fieldToUpdate, String newValue,
                                          int customerId ) throws SQLException {

        /*
         * Use of PreparedStatement to set up a MySQL query statement to update any field in the customer table as
         * specified by the parameters of this method
         */
        String mySQLQueryFieldUpdate =
                "UPDATE customer SET " + fieldToUpdate  + " = '" + newValue + "' WHERE customer_id = ?;";
        PreparedStatement pstmtFieldUpdate = connection.prepareStatement(mySQLQueryFieldUpdate);
        pstmtFieldUpdate.setInt(1, customerId);

        /*
         * Execution of the statement and determination if the update was successful or not based on the return value
         *  of the method.
         */
        rowsAffected = pstmtFieldUpdate.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("The update was successful.\n");
        } else {
            System.out.println("The update could not be done. Please check that the customer ID is correct.\n");
        }

        /* Closing resource to prevent resource leaking. */
        pstmtFieldUpdate.close();

    }
}