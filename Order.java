import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Scanner;

import static java.sql.Types.NULL;


public class Order {

    //Attributes
    int customerId;
    int restaurantId;
    double totalCost;
    int driverId;
    boolean finalised;

    // Global variable
    public static int rowsAffected;

    //Methods

    /**
     * The constructor for the Order class. It is not used but has been included for the sake of completeness.
     *
     * @param customerId The customer's unique ID number or code. Generated automatically upon registration.
     * @param restaurantId The restaurant's unique ID number or code. Generated automatically upon registration.
     * @param totalCost The total cost of all "lines" on a specific order. Calculated from the values in the
     *                  <code>items_order</code> table.
     * @param driverId The driver's unique ID number or code. Generated automatically upon registration.
     * @param finalised A boolean value so that an order can be "signed off" as complete when delivered and paid.
     */
    public Order(int customerId, int restaurantId, double totalCost, int driverId, boolean finalised) {
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.totalCost = totalCost;
        this.driverId = driverId;
        this.finalised = finalised;
    }

    /**
     * This method opens an order and allocates an order number to it. At this stage the order will only have an order
     * number, customer id and restaurant id allocated to it. The rest of the details of the order is still to be
     * populated.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>Customer.findCustomerId</code> and <code>Restaurant.findRestaurantId</code> methods as
     *                   well as the <code>PreparedStatement</code>.
     * @param customerFirstName The customer's first name needed to determine the customer id.
     * @param customerSurname The customer's surname, also needed to determine the customer id.
     * @param restaurantName The restaurant name needed to determine the restaurant if.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static int openOrder(Connection connection, String customerFirstName,
                                 String customerSurname, String restaurantName) throws SQLException {

        /* Declaration and initialisation of the return variable from this method.*/
        int orderNumber = 0;

        /*
         * Obtaining these two variables by invocation of two utility methods from the Customer and Restaurant
         * classes respectively.
         */
        int customerId = Customer.findCustomerId(connection, customerFirstName, customerSurname);
        int restaurantId = Restaurant.findRestaurantId(connection, restaurantName);

        /*
         * The use of PreparedStatement to insert the variables into a MySQL statement. The null variables will be
         * populated later in the program.
         */
        String mySQLQueryOpenOrder = "INSERT INTO `orders` (customer_id, restaurant_id, total_cost, driver_id, " +
                "finalised) " +
                "VALUES (?,?,?,?,?);";
        PreparedStatement pstmtOpenOrder = connection.prepareStatement(mySQLQueryOpenOrder);
        pstmtOpenOrder.setInt(1, customerId);
        pstmtOpenOrder.setInt(2, restaurantId);
        pstmtOpenOrder.setString(3, null);
        pstmtOpenOrder.setString(4, null);
        pstmtOpenOrder.setString(5, null);

        /*
         * A try/catch block to intercept cases where a non-existent customer or restaurant ID is entered.
         */
        try {

                /*
                 * The execution of the statement and setting the return equal to a variable. If this variable is > 0 then
                 * the execution was successful, so it is used here as a check and feedback to the user.
                 */
                rowsAffected = pstmtOpenOrder.executeUpdate();
                if (rowsAffected >0) {
                    System.out.println("Your order was successfully opened.");
                } else {
                    System.out.println("Your order could not be opened. Please review your input.");
                }

            } catch (SQLIntegrityConstraintViolationException e) {
                System.out.println("Order.openOrder: You tried to enter non-existent reference data. Please make sure the " +
                        "client and restaurant you entered, exists in the database.\n");
            }

            /* Ref: https://stackoverflow.com/questions/39287353/java-spring-jdbc-last-insert-id. Reference used to
            retrieve last auto incremented after MySQL query. PreparedStatement to retrieve the last auto_incremented
             value of order_number in the orders table.
             */
            String MySQLOrderNumber = "SELECT MAX(order_number) AS id FROM orders";
            PreparedStatement pstmtOrderNumber = connection.prepareStatement(MySQLOrderNumber);

            /* Execution of the PreparedStatement and storage of the return as a ResultSet.*/
            ResultSet resultsOrderNumber = pstmtOrderNumber.executeQuery();

            /*Extracting the order number with the use of the ResultSet's getter method.*/
            while (resultsOrderNumber.next()) {
                orderNumber = resultsOrderNumber.getInt("id");
            }

            /* Closed resources to prevent resource leak.*/
            pstmtOpenOrder.close();
            pstmtOrderNumber.close();

        return orderNumber;

    }

    /**
     * This method is used to search for an order number. When a customer firstname and surname is used there is a
     * possibility of getting multiple order numbers, therefore the method <code>findExistingOrderNumber</code> is
     * relied on to help with that scenario where the multiple order numbers are stored in an ArrayList and returned
     * to this method for display and user choice.
     *
     * @param connection The Connection resource from the <code>main</code> method needed for the invocation of
     *                   <code>findExistingOrderNumber</code> and <code>displayOrder</code>.
     * @param input The Scanner instance from the <code>main</code> method needed for user input with the help of the
     *             utility class <code>UserInput</code>.
     * @return The required order number searched for by customer first and surname and restaurant name.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static int returnOrderNumber(Connection connection, Scanner input) throws SQLException {

        /* Declaration and initialisation of the orderNumber so that it is accessible outside the if/else if block.*/
        int orderNumber = 0;

        /*
         * Determining which route to follow to get the order number into the method. If it is known by the user, it
         * will simply be read from user input. If not, then the "findExistingOrderNumber" method in the Order class
         * will be used to find it.
         */
        String searchMethodPrompt = "Do you have an order Number? (Y/N)";
        String searchMethod = UserInput.readString(searchMethodPrompt, input);

        /* Setting of the order number. */
        if (searchMethod.equalsIgnoreCase("y")) {
            orderNumber = UserInput.readInteger("Please enter the order number: ", input);
        } else if (searchMethod.equalsIgnoreCase("n")) {
            /*
             * Finding the order number via customer firstname, surname and restaurant name using the method below.
             * An ArrayList is returned because it is possible for a customer to have multiple orders.
             */
            ArrayList<Integer> orderNumbers = findExistingOrderNumber(connection, input);
            String headingForOrderDisplayList = """
                        We have the following orders on record for
                        that combination of customer name and
                        restaurant.""";
            System.out.println(headingForOrderDisplayList);
            for (Integer number : orderNumbers) {
                if (number != 0) {
                    displayOrder(connection, number);
                }
            }
            orderNumber = UserInput.readInteger("Choose an order number: ", input);
        }

        return orderNumber;

    }

    /**
     * This method is called from the <code>captureNewOrder</code> method invoked in the <code>main</code> method of
     * a new order and fills in the driver allocation, updates the driver's workload; it also updates the total cost
     * of the order also generated in the <code>captureNewOrder</code> method. The order has all of its initial
     * details after this method has been invoked.
     *
     * @param connection The Connection resource from the <code>main</code> method which is needed for the invocation
     *                  of the <code>Driver.driverAllocation</code> and <code>Driver.updateDriverWorkload</code>
     *                   methods as well as the instances of <code>PreparedStatement</code>.
     * @param restaurantName The name of the restaurant needed to determine the restaurant ID in order to allocate a
     *                       driver in that area.
     * @param orderNumber The order number needed to update with the total cost and allocated driver.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void addInitialOrderDetails(Connection connection, String restaurantName, int orderNumber) throws SQLException {

        /*Declaration and initialisation of a variable to calculate the grand total cost of an order number. */
        double grandTotalCost = 0.00;

        /* Allocating a driver and updating that driver's workload accordingly with these two methods. */
        int driverId = Driver.driverAllocation(connection, Restaurant.findRestaurantLocation(connection,
                restaurantName));
        Driver.updateDriverWorkload(connection, "add", driverId);

        /*
         * Using PreparedStatement to get a ResultSet back of all the item_costs for a specific order number so that
         * these can be summed to get the grand total of the order.
         */
        String mySQLQueryItemCost = "SELECT item_cost FROM items_order WHERE order_number = ?;";
        PreparedStatement pstmtItemCost = connection.prepareStatement(mySQLQueryItemCost);
        pstmtItemCost.setInt(1, orderNumber);

        /*
         * Using the ResultSet and the grandTotalCost variable to calculate the total cost of an order from the costs
         * of the individual items.
         */
        ResultSet resultsItemCost = pstmtItemCost.executeQuery();
        while (resultsItemCost.next()) {
            grandTotalCost += resultsItemCost.getDouble("item_cost");
        }

        /* Using a PreparedStatement to update the order with the total cost and driver id. */
        String mySQLQueryUpdateOrderCost = "UPDATE orders SET total_cost = ?, driver_id = ? WHERE order_number = ?;";
        PreparedStatement pstmtUpdateOrderCost = connection.prepareStatement(mySQLQueryUpdateOrderCost);
        pstmtUpdateOrderCost.setDouble(1, grandTotalCost);
        pstmtUpdateOrderCost.setInt(2, driverId);
        pstmtUpdateOrderCost.setInt(3, orderNumber);

        /*
         * Execution of the update and then determining if the update was successful based on the return value of the
         * method.
         */
        rowsAffected = pstmtUpdateOrderCost.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("Your order has been updated successfully.\n");
        } else {
            System.out.println("The order could not be updated. Please review your input.\n");
        }

        /* Closing of resources to prevent resource leaking;*/
        pstmtItemCost.close();
        pstmtUpdateOrderCost.close();
        resultsItemCost.close();

    }

    /**
     * A method to update a table with the total cost based on the items_order table that indexes all costs for a
     * specific order. The <code>orders</code> table is then updated with this cost which is the total cost of the
     * order.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the instances of
     *                   <code>PreparedStatement</code>.
     * @param orderNumber The order number to gain access to the correct records in both the <code>items_order</code>
     *                   and <code>orders</code> tables.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void updateWithNewItems(Connection connection, int orderNumber) throws SQLException {

        /* Declaration and initialisation of variable needed to calculate the total cost of the order. */
        double grandTotalCost = 0.00;

        /* PreparedStatement to select the item_cost from each item for a specific order. */
        String mySQLQueryCalculateCost = "SELECT item_cost FROM items_order WHERE order_number = ?;";
        PreparedStatement pstmtCalculateCost = connection.prepareStatement(mySQLQueryCalculateCost);
        pstmtCalculateCost.setInt(1, orderNumber);

        /*
         * Execution of the PreparedStatement and then using the resultant ResultSet to calculate the new grand total
         * cost of the order.
         */
        ResultSet resultsCalculateCost = pstmtCalculateCost.executeQuery();
        while (resultsCalculateCost.next()) {
            grandTotalCost += resultsCalculateCost.getDouble("item_cost");
        }

        /* PreparedStatement to update the orders table with the new total cost. */
        String mySQLUpdate = "UPDATE orders SET total_cost = ? WHERE order_number = ?;";
        PreparedStatement pstmtUpdateWithCost = connection.prepareStatement(mySQLUpdate);
        pstmtUpdateWithCost.setDouble(1, grandTotalCost);
        pstmtUpdateWithCost.setInt(2, orderNumber);

        /*Execution and determination if the update was successful. */
        rowsAffected = pstmtUpdateWithCost.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("The order was successfully updated.\n");
        } else {
            System.out.println("The order could not be updated. Please review your input.\n");
        }

        /* Closing of resources to prevent resource leaking. */
        pstmtUpdateWithCost.close();
        pstmtCalculateCost.close();
        resultsCalculateCost.close();

    }

    /**
     * A method to display the order details in various situations in the program.
     * @param connection The Connection resource from the <code>main</code> program needed for the instances of
     *                   <code>PreparedStatement</code> as well as the <code>Customer.findCustomerName</code>,
     *                   <code>Restaurant.findRestaurantName</code>, <code>Driver.findDriverName</code>,  <code>Item
     *                   .findItemName</code> and <code>Item.findItemPrice</code> methods.
     * @param orderNumber The order number to use as entry point to all the information that needs to be displayed.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void displayOrder(Connection connection, int orderNumber) throws SQLException {

        /* Declaration and initialisation of variables needed from the orders table. */
        int customerId = 0;
        int restaurantId = 0;
        double totalCost = 0.00;
        int driverId = 0;
        boolean finalised = false;

        /* PreparedStatement to select all fields for a specific record identified by an order number. */
        String mySQLQueryDisplayOrder = "SELECT * FROM orders WHERE order_number = ?;";
        PreparedStatement pstmtDisplayOrder = connection.prepareStatement(mySQLQueryDisplayOrder);
        pstmtDisplayOrder.setInt(1, orderNumber);

        /*Execution and saving the return value from the method in a ResultSet. The getter method from the ResultSet
        is then used to obtain the required variables. */
        ResultSet resultsDisplayOrder = pstmtDisplayOrder.executeQuery();
        while (resultsDisplayOrder.next()) {
            customerId = resultsDisplayOrder.getInt("customer_id");
            restaurantId = resultsDisplayOrder.getInt("restaurant_id");
            totalCost = resultsDisplayOrder.getFloat("total_cost");
            driverId = resultsDisplayOrder.getInt("driver_id");
            finalised = resultsDisplayOrder.getBoolean("finalised");
        }

        /*Declaration of variables needed from the ResultSet later in the method. */
        int itemId;
        String specialInstructions;
        int itemQuantity;

        /*PreparedStatement to select all fields from the items_order table for a specific order_number.*/
        String mySQLQueryDisplayItems = "SELECT * FROM items_order WHERE order_number = ?;";
        PreparedStatement pstmtDisplayItems = connection.prepareStatement(mySQLQueryDisplayItems);
        pstmtDisplayItems.setInt(1, orderNumber);

        /*Executing the statement and saving the return value in a ResultSet for use later on. */
        ResultSet resultsDisplayItems = pstmtDisplayItems.executeQuery();

        /* The display of the order details.*/
        System.out.println("""
                Order Details
                -------------
                """);
        System.out.println("Order Number: " + orderNumber);
        System.out.println("Customer Name: " + Customer.findCustomerName(connection, customerId));
        System.out.println("Restaurant Name: " + Restaurant.findRestaurantName(connection, restaurantId));
        String twoDecimalFigure = String.format("%.2f", totalCost);
        System.out.println("Total Cost: R " + twoDecimalFigure);
        System.out.println("Allocated Driver: " + Driver.findDriverName(connection, driverId) + "\n");
        System.out.println("Finalised? " + finalised);
        /* Display of the line items of the order using a while loop because there might be more than one item. A
        combination of the ResultSet's getter method and methods from the Item class is used to get the values. It is
        finally rendered to the screen as a concatenated string. */
        while (resultsDisplayItems.next()) {
            itemQuantity = resultsDisplayItems.getInt("item_quantity");
            itemId = resultsDisplayItems.getInt("item_id");
            String itemName = Item.findItemName(connection, itemId);
            double itemPrice = Item.findItemPrice(connection, itemId);
            String formattedPrice = String.format("%.2f", itemPrice);
            specialInstructions = resultsDisplayItems.getString("preparation_instructions");
            System.out.print(itemQuantity + " x " + itemName + "(" + formattedPrice + ") " + "Special " +
                    "Instructions: " + specialInstructions + "\n");
        }

        System.out.println("\n");

        /*Closing of resources to prevent resource leaking.*/
        pstmtDisplayOrder.close();
        pstmtDisplayItems.close();
        resultsDisplayOrder.close();
        resultsDisplayItems.close();

    }

    /**
     * A method that searches for the order number using the customer's first and surname as well as the restaurant's
     * name to try and eliminate duplicates as far as possible. The order numbers are then stored in an ArrayList
     * which is returned from the method and used in the <code>returnOrderNumber</code> method which is discussed
     * under that methods comment.
     *
     * @param connection The Connection resource from the <code>main</code> menu needed here for the instances of
     *                   <code>PreparedStatement</code>.
     * @param input The Scanner instance from the <code>main</code> menu which is needed to read user input with the
     *              help of the <code>UserInput</code> utility class.
     * @return An ArrayList of order numbers whether 0 or multiple order numbers.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static ArrayList<Integer> findExistingOrderNumber(Connection connection, Scanner input) throws SQLException {

        /*
         * Declaration of a ArrayList to store the order numbers in because there is a chance that a customer may
         * have more than one order even at the same restaurant.
         */
        ArrayList<Integer> orderNumbers= new ArrayList<>();

        /* User input of information needed for this search method. */
        String orderInformationPrompt = "Please enter the following information.";
        System.out.println(orderInformationPrompt);
        String customerFirstName = UserInput.readString("Customer First Name: ", input);
        String customerSurname = UserInput.readString("Customer Surname: ", input);
        String restaurantName = UserInput.readString("Restaurant Name: ", input);

        /*
         * A PreparedStatement that searches for the customer_id in the customer table matching the first and last
         * name of the customer. It then executes the statement which returns a ResultSet whose getter method is used
         * for the customer_id.
         */
        String mySQLQueryCustomerId = "SELECT customer_id FROM customer WHERE LOWER(customer_firstname) = LOWER(?) " +
                "AND LOWER(customer_surname) = LOWER(?);";
        PreparedStatement pstmtCustomerId = connection.prepareStatement(mySQLQueryCustomerId);
        pstmtCustomerId.setString(1, customerFirstName);
        pstmtCustomerId.setString(2, customerSurname);
        ResultSet resultsCustomerId = pstmtCustomerId.executeQuery();
        int customerId = NULL;
        while (resultsCustomerId.next()) {
            customerId = resultsCustomerId.getInt("customer_id");
        }

        /*
         * A PreparedStatement that searches for the restaurant_id in the restaurant table using the restaurant name.
         *  It then executes the statement and also function as the statement above.
         */
        String mySQLQueryRestaurantId = "SELECT restaurant_id FROM restaurant WHERE LOWER(restaurant_name) = LOWER(?);";
        PreparedStatement pstmtRestaurantId = connection.prepareStatement(mySQLQueryRestaurantId);
        pstmtRestaurantId.setString(1, restaurantName);
        ResultSet resultsRestaurantId = pstmtRestaurantId.executeQuery();
        int restaurantId = NULL;
        while (resultsRestaurantId.next()) {
            restaurantId = resultsRestaurantId.getInt("restaurant_id");
        }

        /*
         * Finally a PreparedStatement is used again to select the order_number where the customerId and restaurantId
         *  matches those determined above. The order_number is found like above by the getter method of the
         * ResultSet that was returned from the execution of the statement.
         */
        String mySQLQueryOrderNumbers = "SELECT order_number FROM orders WHERE customer_id = ? AND restaurant_id = ?;";
        PreparedStatement pstmtOrderNumbers = connection.prepareStatement(mySQLQueryOrderNumbers);
        pstmtOrderNumbers.setInt(1, customerId);
        pstmtOrderNumbers.setInt(2, restaurantId);
        ResultSet resultsOrderNumbers = pstmtOrderNumbers.executeQuery();
        while (resultsOrderNumbers.next()) {
            orderNumbers.add(resultsOrderNumbers.getInt("order_number"));
        }

        /* Closing of resources to prevent resource leaking.*/
        pstmtOrderNumbers.close();
        pstmtCustomerId.close();
        pstmtRestaurantId.close();
        resultsOrderNumbers.close();
        resultsCustomerId.close();
        resultsRestaurantId.close();

        return orderNumbers;

    }

    /**
     * This method checks for orders with incomplete information based on null values in the <code>orders</code>
     * table. There is also a similar method that checks for incomplete customer information in the
     * <code>Customer</code> class. The assumption is that the restaurant, menu items and driver information is
     * relatively fixed so, it is not checked everytime.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @return An ArrayList with the numbers of the incomplete orders found.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static ArrayList<Integer> checkForIncompleteOrders(Connection connection) throws SQLException {

        /* Declaration of an ArrayList to store the list of order numbers that will be produced by this method. */
        ArrayList<Integer> incompleteOrderNumbers = new ArrayList<>();

        /*
         * A PreparedStatement to select all records in the orders table where the total_cost and driver_id is NULL.
         * Only these two fields are checked because the other three columns will always have data in them since the
         * first is an AUTO_INCREMENT column and the others two are forced to have data because of the UserInput
         * class that is used to read their input. The ResultSet that is returned after execution of the statement
         * will return multiple records so each of these will be added to the ArrayList incompleteOrderNumbers.
         */
        String mySQLQueryOrders = "SELECT * FROM orders WHERE total_cost IS NULL AND driver_id IS NULL;";
        PreparedStatement pstmtOrders = connection.prepareStatement(mySQLQueryOrders);
        ResultSet resultsOrders = pstmtOrders.executeQuery();
        while (resultsOrders.next()) {
            incompleteOrderNumbers.add(resultsOrders.getInt("order_number"));
        }

        /* Closing of resources to prevent resource leaking. */
        pstmtOrders.close();
        resultsOrders.close();

        return incompleteOrderNumbers;
    }

    /**
     * A method that finds pending orders by searching through the boolean column <code>finalised</code> in the
     * <code>orders</code> table and returns a list of records that have false values.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @return An ArrayList of the order numbers of the pending orders.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static ArrayList<Integer> findPendingOrders(Connection connection) throws SQLException {

        /*Declaration of an ArrayList to store the possibly multiple order numbers that will result from this query.*/
        ArrayList<Integer> pendingOrders = new ArrayList<>();

        /*
         * A PreparedStatement that selects all order_numbers of records where the finalised value is false (TINYINT
         * = 0). The statement will be executed and a ResultSet will be returned which will be accessed with a while
         * loop. All order_numbers found will be added to the ArrayList.
         */
        String mySQLPendingOrders = "SELECT order_number FROM orders WHERE finalised IS FALSE;";
        PreparedStatement pstmtPendingOrders = connection.prepareStatement(mySQLPendingOrders);
        ResultSet resultsPendingOrders = pstmtPendingOrders.executeQuery();
        while (resultsPendingOrders.next()) {
            pendingOrders.add(resultsPendingOrders.getInt("order_number"));
        }

        /* Closing of resources to prevent resource leaking. */
        pstmtPendingOrders.close();
        resultsPendingOrders.close();

        return pendingOrders;

    }

    /**
     * This method changes the <code>finalised</code> value of a record in the <code>orders</code> table to true when
     * am order is completed. It also updates the allocated driver's workload by invoking  <code>Driver
     * .updateDriverWorkload</code> and prints an invoice to file by invoking <code>printInvoice</code>.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the instances of
     *                   <code>PreparedStatement</code> and the two methods <code>Driver
     *                   .updateDriverWorkload</code> <code>printInvoice</code>
     * @param orderNumber The order number that is used as the entry point to carry out these functions.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void makeFinal(Connection connection, int orderNumber) throws SQLException {

        /* Declaration and initialisation of driverId variable because it is needed outside the while loop. */
        int driverId = 0;

        /*
         * A PreparedStatement to update a given order number so that the finalised variable is true (TINYINT = 1).
         */
        String mySQLFinaliseOrder = "UPDATE orders SET finalised = true WHERE order_number = ?;";
        PreparedStatement pstmtFinaliseOrder = connection.prepareStatement(mySQLFinaliseOrder);
        pstmtFinaliseOrder.setInt(1, orderNumber);

        /*Execution of the statement and determination if the update was successful.*/
        rowsAffected = pstmtFinaliseOrder.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("The order was successfully finalised.");
        } else {
            System.out.println("The operation could not be performed. Check if you entered a valid order number.");
        }

        /*
         * A PreparedStatement to find the driver id of the same order number to be used below.
         */
        String mySQLQueryFindDriverId = "SELECT driver_id FROM orders WHERE order_number = ?;";
        PreparedStatement pstmtFindDriverId = connection.prepareStatement(mySQLQueryFindDriverId);
        pstmtFindDriverId.setInt(1, orderNumber);
        ResultSet resultsFindDriverId = pstmtFindDriverId.executeQuery();
        while(resultsFindDriverId.next()) {
            driverId = resultsFindDriverId.getInt("driver_id");
        }

        /* The update of the driver's workload. I.e., 1 Order will be removed.*/
        Driver.updateDriverWorkload(connection, "remove", driverId);

        /* Printing of an invoice to file as required by the client.*/
        printInvoice(connection, orderNumber);

        /* Closing of resources to prevent resource leaking.*/
        pstmtFinaliseOrder.close();
        pstmtFindDriverId.close();
        resultsFindDriverId.close();
    }

    /**
     * This method handles the printing of the invoice in a format determined by the fictional company.
     *
     * @param connection The Connection resource from the <code>main</code> method that is used extensively
     *                   throughout this method both for <code>PreparedStatement</code> and other method invocation
     *                   which will not be discussed individually.
     * @param orderNumber The order number of the particular record in the <code>orders</code> table which is the
     *                    "index" table of the database. All information can be accessed through this order number.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void printInvoice(Connection connection, int orderNumber) throws SQLException {

        /*Declaration and initialisation of all variables needed for some sections of the invoice. */
        int customerId = 0;
        int restaurantId = 0;
        double totalCost = 0.00;
        int driverId = 0;
        String customerEmail = "";
        String customerPhoneNumber = "";
        String customerCity = "";
        String customerAddress = "";

        /*Instantiation of a StringWriter and PrintWriter objects to be used in the writeInvoiceToFile method*/
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        /*
         * The process of using PreparedStatements has already been discussed. It will not be discussed again here.
         * This one is to get the information from the orders table for a given order_number. */
        String mySQLQueryOrder = "SELECT * FROM orders WHERE order_number = ?;";
        PreparedStatement pstmtOrderInformation = connection.prepareStatement(mySQLQueryOrder);
        pstmtOrderInformation.setInt(1, orderNumber);
        ResultSet resultsOrderInformation = pstmtOrderInformation.executeQuery();
        while (resultsOrderInformation.next()) {
            customerId = resultsOrderInformation.getInt("customer_id");
            restaurantId = resultsOrderInformation.getInt("restaurant_id");
            totalCost = resultsOrderInformation.getFloat("total_cost");
            driverId = resultsOrderInformation.getInt("driver_id");
        }

        /* Declaration of variables that will be accessed through resultsItemsInformation later in the method. */
        int itemId;
        String specialInstructions;
        int itemQuantity;

        /*
         * This one is for the items_order table's information for a given order_number.
         */
        String mySQLQueryItems = "SELECT * FROM items_order WHERE order_number = ?;";
        PreparedStatement pstmtItemsInformation = connection.prepareStatement(mySQLQueryItems);
        pstmtItemsInformation.setInt(1, orderNumber);
        ResultSet resultsItemsInformation = pstmtItemsInformation.executeQuery();

        /*
         * This one is for the customer table's information for a given customer_id
         */
        String mySQLQueryCustomer = "SELECT * FROM customer WHERE customer_id = ?;";
        PreparedStatement pstmtCustomer = connection.prepareStatement(mySQLQueryCustomer);
        pstmtCustomer.setInt(1, customerId);
        ResultSet resultsCustomer = pstmtCustomer.executeQuery();
        while (resultsCustomer.next()) {
            customerEmail = resultsCustomer.getString("customer_email");
            customerPhoneNumber = resultsCustomer.getString("customer_phone_num");
            customerCity = resultsCustomer.getString("customer_city");
            customerAddress = resultsCustomer.getString("customer_address");
        }

        /* The actual preparation of the output using the printWriter instance. */
        printWriter.println("Invoice\n");
        printWriter.println("Order Number: " + orderNumber);
        printWriter.println("Customer: " + Customer.findCustomerName(connection, customerId));
        printWriter.println("Email: " + customerEmail);
        printWriter.println(("Phone number: " + customerPhoneNumber));
        printWriter.println("Location: " + customerCity);
        printWriter.println("\n");
        printWriter.println("You have ordered the following from " + Restaurant.findRestaurantName(connection,
                restaurantId) + " in " + Restaurant.findRestaurantLocation(connection,
                Restaurant.findRestaurantName(connection, restaurantId)) + ":");
        printWriter.println("\n");
        /* A while loop because there is likely more than one item on the order. */
        while (resultsItemsInformation.next()) {
            itemQuantity = resultsItemsInformation.getInt("item_quantity");
            itemId = resultsItemsInformation.getInt("item_id");
            String itemName = Item.findItemName(connection, itemId);
            double itemPrice = Item.findItemPrice(connection, itemId);
            String formattedPrice = String.format("%.2f", itemPrice);
            specialInstructions = resultsItemsInformation.getString("preparation_instructions");
            printWriter.print(itemQuantity + " x " + itemName + " (R" + formattedPrice + ") " + "Special " +
                    "Instructions: " + specialInstructions + "\n");
        }
        printWriter.println("\n");
        String twoDecimalFigure = String.format("%.2f", totalCost);
        printWriter.println("Total: " + twoDecimalFigure);
        printWriter.println(Driver.findDriverName(connection, driverId) + " is nearest to the restaurant and so he " +
                "will be delivering your order at:");
        printWriter.println(customerAddress);
        printWriter.println("\n");
        printWriter.println("If you need to contact the restaurant, their number is " + Restaurant.findRestaurantPhoneNumber(connection, restaurantId));

        printWriter.println("\n");

        /*The invocation of the method responsible for writing the information to a file*/
        writeInvoiceToFile(stringWriter.toString());

        /* Closing of resources to prevent resource leaking. */
        pstmtCustomer.close();
        pstmtItemsInformation.close();
        pstmtOrderInformation.close();
        resultsCustomer.close();
        resultsItemsInformation.close();
        resultsOrderInformation.close();
        printWriter.close();

    }

    /**
     * The method by which the invoice is printed to a text file line by line.
     * @param lineToPrint The string that is printed to the indicated file.
     */
    public static void writeInvoiceToFile(String lineToPrint) {
        try {

            /* Name of file to write. */
            String fileToWriteTo = "invoice.txt";

            /*
             * Using an instance of FileWriter to write to the named file. The second argument ensures info is
             * appended and not overwritten.
             */
            FileWriter writer = new FileWriter(fileToWriteTo, true);

            /*
             * Using an instance of Formatter to make sure the information is printed line by line on separate
             * lines in the output file
             */
            Formatter form = new Formatter(writer);
            form.format("%s", lineToPrint + "\n");

            /*Closing of the resources to prevent resource leaking.*/
            form.close();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error: IOException - writeInvoiceToFile()");
        }

    }
}