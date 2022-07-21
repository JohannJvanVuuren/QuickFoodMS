import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * This is a project management system for a fictional food delivery company called Food Quick. The purpose of this
 * system is to keep track of the orders that they are working on at any given time.
 *
 * @author Johann Jansen van Vuuren
 * @version QuickFoodMS-1.0.0
 * @since 2022-07-18
 */
public class QuickFoodMS {


    /**
     * The main method from where all other methods will be invoked based on the user's menu choice.
     *
     * @param args Main method.
     */
    public static void main(String[] args) {

        System.out.println("Welcome to the Quick Food Management System.\n");

        /*
         * Scanner instance created in main to keep input stream open despite the number of calls of the
         * "readUserInput", "readUserInputInteger" and "readUserInputDouble" methods.
         */
        Scanner scanner = new Scanner(System.in);

        /* Variables for use in opening the JDBC connection to the "PoisePMS_db" database. */
        String dbURL = "jdbc:mysql://localhost:3306/QuickFoodMS_db?useSSL=false";
        String username = "otheruser";
        String password = "swordfish";

        /* Using a try with resources / catch block to open the Connection resource and catch any SQL Exceptions. */
        try (Connection connection = DriverManager.getConnection(dbURL, username, password)) {

            /*
             * A while loop that will continue running until the user chooses to exit the program by selecting the
             * relevant option number below.
             */
            while (true) {

                /* Main menu of the application. */
                String promptMenuChoice = """
                        Main Menu (Select the Number Next to Your Choice):
                        --------------------------------------------------
                        1. Capture New Order.
                        2. Add Items To Existing Order.
                        3. Find and Display Order.
                        4. List Orders With Incomplete Information.
                        5. List Pending Orders.
                        6. List Orders Allocated To A Specific Driver.
                        7. Finalise Order.
                        8. Capture New Customer.
                        9. Update Existing Customers.
                        10. Capture New Restaurant.
                        11. Update Existing Restaurant.
                        12. Add New Menu Item.
                        13. Edit Menu Item
                        14. Capture New Driver.
                        15. Update Driver.
                        16. Exit
                        """;

                /*
                 * Reading of user input of their menu choice and an if statement to handle invalid numbers selected.
                 */
                int menuSelection = UserInput.readInteger(promptMenuChoice, scanner);
                if (menuSelection < 1 || menuSelection > 16) {
                    System.out.println("Invalid number entered.");
                    continue;
                }


                /*
                 * A switch statement to handle the different menu choices and to delegate them to specific methods
                 * to handle the respective choices and functionalities associated with them.
                 * Ref: https://docs.oracle.com/en/java/javase/13/language/switch-expressions.html. NOTE TO
                 * REVIEWER: I spoke to Pierre, and he gave the go ahead to use the new enhanced switch statement as
                 * long as it is referenced.
                 */
                switch (menuSelection) {
                    case 1 -> captureNewOrder(connection, scanner);
                    case 2 -> addItemsToExistingOrder(connection, scanner);
                    case 3 -> findAndDisplayOrder(connection, scanner);
                    case 4 -> listOrdersWithIncompleteInfo(connection);
                    case 5 -> listPendingOrders(connection);
                    case 6 -> ordersAllocatedToDriver(connection, scanner);
                    case 7 -> finaliseOrder(connection, scanner);
                    case 8 -> captureNewCustomer(connection, scanner);
                    case 9 -> updateCustomer(connection, scanner);
                    case 10 -> captureNewRestaurant(connection, scanner);
                    case 11 -> updateRestaurant(connection, scanner);
                    case 12 -> addNewItem(connection, scanner);
                    case 13 -> editItem(connection, scanner);
                    case 14 -> captureNewDriver(connection, scanner);
                    case 15 -> editDriver(connection, scanner);
                    case 16 -> {
                        System.out.println("Thank you for using the QuickFoodMS application. Bye.");
                        System.exit(0);
                    }
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQL Exception thrown in main.\n");
        }

        /* Closing this resource to prevent resource leaking. */
        scanner.close();

    }

    /**
     * A method to capture new orders. This method will check if a customer is already in the database and if not it
     * will divert the user to the <code>createNewCustomer</code> method in the Customer class.
     *
     * @param connection The Connection instance from the <code>main</code> method needed for
     *                   <code>PreparedStatement</code>.
     * @param input      The Scanner instance from <code>main</code> needed for the <code>readUserInput</code>
     *                   and <code>readUserInputInteger</code>  methods.
     * @throws SQLException If underlying MySQL service fails.
     */
    public static void captureNewOrder(Connection connection, Scanner input) throws SQLException {

        /* Determine if this is an existing customer. */
        String existingCustomer = UserInput.readString("Is this an existing customer? (Y/N)", input);

        /* Collection of the first and surname here, because it is needed in both use cases. */
        String customerFirstName = UserInput.readString("Customer First Name: ", input);
        String customerSurname = UserInput.readString("Customer Surname: ", input);

        /*
         * If an existing customer then the order can be placed straight away, else the customer must first be
         * captured and stored to the "customer" table.
         */
        if (existingCustomer.equalsIgnoreCase("n")) {

            /* Adding a new customer to the customer table by means of the Customer class' createNewCustomer method. */
            Customer.createNewCustomer(connection, input, customerFirstName, customerSurname);

        }

        /*
         * Reading the restaurant name after the customer creation to keep the collection of the customer
         * information separate from the restaurant name collection.
         */
        String restaurantName = UserInput.readString("Restaurant Name: ", input);

        /* Opening of an order with only the order-number, customer_id and restaurant_id columns being populated. */
        int orderNumber = Order.openOrder(connection, customerFirstName, customerSurname, restaurantName);

        /* The "add item to order" while loop that will continue until a user enters the finished option. */
        while (true) {

            /* Add items to order menu. */
            String addItemsToOrderPrompt = """
                    Add Items Menu
                    --------------
                    1. Add Item.
                    2. Exit
                    """;

            /* Reading the menu choice */
            int addItemsToOrder = UserInput.readInteger(addItemsToOrderPrompt, input);

            /* Handling invalid menu choices. */
            if (addItemsToOrder < 1 || addItemsToOrder > 2) {
                System.out.println("Invalid option selected.\n");
            }

            /* Handling of valid menu choices.*/
            if (addItemsToOrder == 1) {
                ItemsOrder.addItemToOrder(connection, input, orderNumber);
            } else if (addItemsToOrder == 2) {
                break;
            }

        }

        /* Update the "orders" table with the total cost and allocating a driver. */
        Order.addInitialOrderDetails(connection, restaurantName, orderNumber);

        /* Displaying the order details. */
        Order.displayOrder(connection, orderNumber);
    }

    /**
     * A method to add items to the items-order table and then to update the order in the orders table. It makes use
     * of the returnOrderNumber and updateWithNewItems methods from the Order class as well as addItemToOrder from
     * the ItemsOrder class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed as a parameter for the
     *                   invocation of the class methods.
     * @param input The Scanner instance from the <code>main</code> method needed for the two invocation of the
     *              UserInput methods that require the instance as arguments when invoked.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void addItemsToExistingOrder(Connection connection, Scanner input) throws SQLException {

        /* Obtaining the order number either directly or from the customer names and restaurant names. */
        int orderNumber = Order.returnOrderNumber(connection, input);

        /*
         * Using these two methods from the respective classes to first add the item to the items_order table and
         * then to update the orders table's cost column accordingly. Invalid or unknown order numbers will trigger
         * error messages from within the first of these methods.
         */
        ItemsOrder.addItemToOrder(connection, input, orderNumber);
        Order.updateWithNewItems(connection, orderNumber);
    }

    /**
     * This method locates and display the order information based on user input. It can locate the order by either
     * order number or customer names.
     *
     * @param connection The Connection resource from the <code>main</code> method needed for the connection
     *                   arguments of both method calls.
     * @param input The Scanner instance from the <code>main</code> method needed for user input by the
     *              <code>Order.returnOrderNumber</code> method call.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void findAndDisplayOrder(Connection connection, Scanner input) throws SQLException {

        /*
         * Fetching of the order number by the invocation of the returnOrderNumber method from the Order class. This
         * method can take in either the order number if it is known or the customer and restaurant details if the
         * order number is unknown.
         */
        int orderNumber = Order.returnOrderNumber(connection, input);

        /*
         * Displaying the order details once found. If there are more than one order that fits the search criteria
         * then a list of the orders will be shown from which the user can choose.
         */
        Order.displayOrder(connection, orderNumber);

    }

    /**
     * A method to find and display orders and/or customers with incomplete information in the database.
     *
     * @param connection The Connection resource from the <code>main</code> method needed for the invocation of all the
     *                   class methods.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void listOrdersWithIncompleteInfo(Connection connection) throws SQLException {

        /*
         * It is assumed that the restaurant and item tables are relatively fixed during the ordering process, so
         * they have been included. The assumption is that adding restaurants or menu items are comparatively rare
         * events.
         */

        /*
         * The method returns an array list of all order numbers that have NULL data cells. If the ArrayList length
         * is 0 then there are no incomplete orders. Anything > 0 indicates incomplete records. These incomplete
         * records are then listed by the use of an advanced for loop and the "displayOrder" method in the Order
         * class. The check for incomplete customer info below works in the same way, so it will not be discussed.
         */
        ArrayList<Integer> incompleteOrders = Order.checkForIncompleteOrders(connection);
        if (incompleteOrders.size() == 0) {
            System.out.println("There are no incomplete orders.\n");
        } else {
            System.out.println("Incomplete Orders: \n");
            for (Integer order : incompleteOrders) {
                Order.displayOrder(connection, order);
            }
            System.out.println("\n");
        }


        ArrayList<Integer> incompleteCustomerInfo = Customer.checkForIncompleteCustomerInfo(connection);
        if (incompleteCustomerInfo.size() == 0) {
            System.out.println("There are no customers with incomplete information.\n");
        } else {
            System.out.println("Incomplete Customer Information: ");
            for (Integer customer : incompleteCustomerInfo) {
                Customer.displayCustomer(connection, customer);
            }
        }


    }

    /**
     * This method finds and list pending orders based on the <code>finalised</code> field in the <code>orders</code>
     * table. It uses two utility methods from the Order class to do so. "findPendingOrders" and "displayOrder".
     * @param connection The Connection resource from the <code>main</code> method needed for the invocation of the
     *                   utility methods.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void listPendingOrders(Connection connection) throws SQLException{

        /*
         * The method returns an ArrayList with all orders numbers of records in the "orders" table that have
         * finalised fields that are false(TINYINT = 0).
         */
        ArrayList<Integer> pendingOrders = Order.findPendingOrders(connection);

        /*
         * If the ArrayList length is 0 then there are no pending orders, else the pending orders are listed by using
         * an advanced for loop and the Order.displayOrder method.
         */
        if (pendingOrders.size() == 0) {
            System.out.println("There are no pending orders.\n");
        } else {
            System.out.println("Pending orders: \n");
            for (Integer order : pendingOrders) {
                Order.displayOrder(connection, order);
            }
        }

    }

    /**
     * This method finds and lists all the orders associated with a specific driver. It uses utility methods from
     * the Driver and Order classes to do so. These methods are <code>findDriverId</code>, <code>findDriverName</code>
     * and <code>findOrdersAllocatedToDriver</code> from the Driver class and <code>displayOrder</code> from the
     * Order class.
     * @param connection The Connection resource from the <code>main</code> method that is needed as arguments for
     *                   the utility methods that are invoked.
     * @param input The Scanner instance from the <code>main</code> method that is needed as an argument for the
     *              utility methods that are invoked.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void ordersAllocatedToDriver(Connection connection, Scanner input) throws SQLException {

        /* The obtaining of the driverId and name by use of the two utility methods in the Driver class. */
        int driverId = Driver.findDriverId(connection, input);
        String driverName = Driver.findDriverName(connection, driverId);

        /*
         * This method returns an ArrayList with the order numbers of all the orders allocated to the specified driver.
         */
        ArrayList<Integer> ordersAllocatedToDriver = Driver.findOrdersAllocatedToDriver(connection, driverId);

        /*
         * If the ArrayList length is 0 then there are none allocated to that driver. If > 0 then the orders are
         * listed with an advanced for loop and the Order.displayOrder method.
         */
        if (ordersAllocatedToDriver.size() == 0) {
            System.out.println("There are currently no orders allocated to " + driverName + "\n");
        } else {
            System.out.println("The following orders are allocated to " + driverName + "\n");
            for (Integer order : ordersAllocatedToDriver) {
                Order.displayOrder(connection, order);
            }
        }

    }

    /**
     * A method to update the status of an order to "finalised" when the order has been completed and paid for. It does
     * this with the help of the utility method in Order class, <code>makeFinal</code>.
     *
     * @param connection The Connection resource from the <code>main</code> method needed when invoking the helper
     *                   method from the Order class.
     * @param input The Scanner instance from the <code>main</code> method needed to read input from the user with
     *              the help of the UserInput class.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void finaliseOrder(Connection connection, Scanner input) throws SQLException {

        /*
         * User input of a orderNumber and then the finalisation of that order by the Order.makeFinal method (which
         * will also write an invoice to file).
         */
        int orderNumber = UserInput.readInteger("Order Number To Finalise: ", input);
        Order.makeFinal(connection, orderNumber);

    }

    /**
     * The capturing of new customers are handled by this method. It does so via the <code>createNewCustomer</code>
     * utility method in the Customer class.
     *
     * @param connection The Connection resource from the <code>main</code> method that is needed as an argument when
     *                  calling the utility method.
     * @param input The Scanner instance from the <code>main</code> method that is needed to read user input via the
     *              UserInput class.
     * @throws SQLException If the underlying MySQL service fails
     */
    public static void captureNewCustomer(Connection connection, Scanner input) throws SQLException {

        /*
         * Input of the customer's first and surname which will be used by the method below to start the capturing of
         * the new customer.
         */
        String customerFirstName = UserInput.readString("Customer First Name: ", input);
        String customerSurname = UserInput.readString("Customer Surname: ", input);

        /*
         * The creation of the customer is almost completely handled by the utility method below to try and keep code
         * as modularised as possible.
         */
        Customer.createNewCustomer(connection, input, customerFirstName, customerSurname);
    }

    /**
     * A method that allows the user to update any of the fields in the customer table. This is done via the
     * performFieldUpdate utility method in the Customer class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the invocation of
     *                   the<code>Customer.performFieldUpdate</code> method.
     * @param input The Scanner instance from the <code>main</code> method needed for invocation of the UserInput
     *              class' methods.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void updateCustomer(Connection connection, Scanner input) throws SQLException {

       /* This while loop will continue running until the user selects 7 from the menu. */
       while (true) {

           /* The Customer Update Menu that will be used together with a switch statement to update the chosen field. */
           String updateMenuPrompt = """
                   Customer Update Menu
                   --------------------
                   (Please indicate the field you wish to update.)
                   1. First Name.
                   2. Surname.
                   3. Phone Number.
                   4. Address.
                   5. Location (City).
                   6. Email.
                   7. Return to Main Menu.
                   """;

           int updateMenuChoice = UserInput.readInteger(updateMenuPrompt, input);

           /* The handling of invalid menu choices */
           if (updateMenuChoice < 1 || updateMenuChoice > 7) {
               System.out.println("invalid choice entered.");
               continue;
           }

           /* The exiting of the while loop and return to the main menu if the user chooses 7.*/
           if (updateMenuChoice == 7) {
               break;
           }

           /* Input of the variables needed as arguments when invoking the Customer.performFieldUpdate method. */
           int customerId = UserInput.readInteger("Customer ID: ", input);
           String newValueOfField = UserInput.readString("What is the new value? ", input);

           /*
            * The switch statement to handle the different menu changes with the "fieldToUpdate" hardcoded based on
            * the menu choice.
            */
           switch (updateMenuChoice) {
               case 1 -> Customer.performFieldUpdate(connection, "customer_firstname", newValueOfField, customerId);
               case 2 -> Customer.performFieldUpdate(connection, "customer_surname", newValueOfField, customerId);
               case 3 -> Customer.performFieldUpdate(connection, "customer_phone_num", newValueOfField, customerId);
               case 4 -> Customer.performFieldUpdate(connection, "customer_address", newValueOfField, customerId);
               case 5 -> Customer.performFieldUpdate(connection, "customer_city", newValueOfField, customerId);
               case 6 -> Customer.performFieldUpdate(connection, "customer_email", newValueOfField, customerId);
           }
       }

    }

    /**
     * A method that captures a new restaurant completely through the <code>createNewRestaurant</code> method in the
     * class Restaurant. It was structured like this so that all functionalities of the program are kept in the same
     * file as the main method that invokes them.
     *
     * @param connection The Connection resource from the <code>main</code> method that is needed as an argument when
     *                  invoking "Restaurant.createNewRestaurant".
     * @param input The Scanner instance from the <code>main</code> method that is also needed for the invocations.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void captureNewRestaurant(Connection connection, Scanner input) throws SQLException {

        /*
         * This method is commented upon in the Restaurant class, but it handles the capturing of the new restaurant.
         * I have again done it this way to keep my code as modular as possible.
         */
        Restaurant.createNewRestaurant(connection, input);

    }

    /**
     * A method that updates any details associated with a restaurant. It does this in conjunction with the
     * <code>performFieldUpdate</code> method from class <code>Restaurant</code>.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>performFieldUpdate</code> invocation.
     * @param input The Scanner instance from the <code>main</code> method needed here to read input with the help of
     *             the <code>UserInput</code> utility class.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void updateRestaurant(Connection connection, Scanner input) throws SQLException {

        /* A while loop that will keep running until the user enters 4 to return to the main menu. */
        while (true) {

            String updateMenuPrompt = """
                   Restaurant Update Menu
                   --------------------
                   (Please indicate the field you wish to update.)
                   1. Restaurant Name.
                   2. Restaurant Phone Number.
                   3. Restaurant City.
                   4. Return to Main Menu.
                   """;

            int updateMenuChoice = UserInput.readInteger(updateMenuPrompt, input);

            /* The handling of invalid menu choices. */
            if (updateMenuChoice < 1 || updateMenuChoice > 4) {
                System.out.println("invalid choice entered.");
                continue;
            }

            /* The break out of the while loop and return to the main menu if the user selects 4. */
            if (updateMenuChoice == 4) {
                break;
            }

            /* User input of the variables needed for the update. */
            int restaurantId = UserInput.readInteger("Restaurant ID: ", input);
            String newValueOfField = UserInput.readString("What is the new value? ", input);

            /*
             * The switch statement linked to the menu choices. The "fieldToUpdate" is hardcoded based on the menu
             * option chosen.
             */
            switch (updateMenuChoice) {
                case 1 -> Restaurant.performFieldUpdate(connection, "restaurant_name", newValueOfField, restaurantId);
                case 2 -> Restaurant.performFieldUpdate(connection, "restaurant_phone_num", newValueOfField,
                        restaurantId);
                case 3 -> Restaurant.performFieldUpdate(connection, "restaurant_city", newValueOfField, restaurantId);
            }
        }

    }

    /**
     * A method to add new items to the menu. It uses the <code>createNewItem</code> method from the
     * <code>Item</code> class to do so. It is structured to be in the same file as the main method because all
     * functionalities of the program is invoked from there.
     *
     * @param connection The Connection resource from the <code>main</code> method, needed as an argument when
     *                   invoking <code>createNewItem</code>.
     * @param input The Scanner instance from the main menu needed for the invocation of <code>createNewItem</code>
     *              as well.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void addNewItem(Connection connection, Scanner input) throws SQLException {

        /*
         * The adding of new items to the menu is completely handled by this method in the Item class to keep the
         * code as modularised as possible.
         */
        Item.createNewItem(connection, input);
    }

    /**
     * This method allows the user to edit any item on the menu. It does so by means of the
     * <code>performFieldUpdate</code> method in the Item class.
     *
     * @param connection The Connection resource from the <code>main</code> method which is needed to invoke the
     *                   <code>performFieldUpdate</code> method.
     * @param input The Scanner instance from the <code>main</code> method needed to read user input with the utility
     *             class <code>UserInput</code>.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void editItem(Connection connection, Scanner input) throws SQLException {

        /* A while loop to keep running until the user selects 3 to return to the main menu. */
        while (true) {

            String updateMenuPrompt = """
                   Edit Menu Item
                   --------------------
                   (Please indicate the field you wish to update.)
                   1. Item Name.
                   2. Item Price.
                   3. Return to Main Menu.
                   """;

            int updateMenuChoice = UserInput.readInteger(updateMenuPrompt, input);

            /* The handling of invalid menu choices. */
            if (updateMenuChoice < 1 || updateMenuChoice > 3) {
                System.out.println("invalid choice entered.");
                continue;
            }

            /* Breaking out of the while loop and returning to the main menu when the user selects 3. */
            if (updateMenuChoice == 3) {
                break;
            }

            /* User input of the item ID. */
            int itemId = UserInput.readInteger("Item ID: ", input);

            /* Declaration and initialisation of the variables needed for the update. */
            String newValueOfFieldString = "";
            double newValueOfFieldDouble = 0.00;

            /*
             * The switch statement linked to menu choices. It is obvious that the handling of the two choices is
             * different because the one value is a String and the other a double. The method therefore takes both a
             * string and double as arguments and use the appropriate one.
             */
            switch (updateMenuChoice) {
                case 1 -> {
                    newValueOfFieldString = UserInput.readString("What is the new value? ", input);
                    String typeOfData = "String";
                    Item.performFieldUpdate(connection, "item_name", newValueOfFieldString,
                            newValueOfFieldDouble, typeOfData, itemId);
                }
                case 2 -> {
                    newValueOfFieldDouble = UserInput.readDouble("What is the new value? ", input);
                    String typeOfData = "Double";
                    Item.performFieldUpdate(connection, "item_price", newValueOfFieldString,
                            newValueOfFieldDouble, typeOfData, itemId);
                }
            }
        }
    }

    /**
     * A method to capture a new driver into the database. The addition is completely handled by the
     * <code>createDriver</code> method in the Driver class.
     *
     * @param connection The Connection resource from the <code>main</code> method that is needed to invoke the
     *                   <code>createDriver</code> method.
     * @param input The Scanner instance from the <code>main</code> method that is also needed for the invocation of
     *              the <code>createDriver</code> method.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void captureNewDriver(Connection connection, Scanner input) throws SQLException {

        /* Capturing a new driver is handled by the Driver class to keep the code as modular as possible. */
        Driver.createDriver(connection, input);

    }

    /**
     * This method allows the user to edit any of the details of an existing driver. It does so with the help of the
     * <code>performFieldUpdate</code> method in the Driver class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed for the invocation of the
     *                   <code>performFieldUpdate</code> method.
     * @param input The Scanner instance from the <code>main</code> method needed to read user input with the help of
     *             the utility class <code>UserInput</code>.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void editDriver(Connection connection, Scanner input) throws SQLException {

        /*
         * A while loop to keep running until the user selects 3 from the menu which will return the user to the main
         * menu.
         */
        while (true) {

            String updateMenuPrompt = """
                   Edit Menu Item
                   --------------------
                   (Please indicate the field you wish to update.)
                   1. Driver Name.
                   2. Driver Location (City).
                   3. Return to Main Menu.
                   """;

            int updateMenuChoice = UserInput.readInteger(updateMenuPrompt, input);

            /* Handling of invalid menu choices. */
            if (updateMenuChoice < 1 || updateMenuChoice > 3) {
                System.out.println("invalid choice entered.");
                continue;
            }

            /* Breaking out of the while loop and returning to the main menu if the user chooses 3 from the menu. */
            if (updateMenuChoice == 3) {
                break;
            }

            /* User input of the variables needed as arguments for the method invoked below. */
            int driverId = UserInput.readInteger("Driver ID: ", input);
            String newValueOfField = UserInput.readString("What is the new value? ", input);

            /* The switch statement that is linked to the menu. The "fieldToUpdate" has been hardcoded based on the
            menu choice. */
            switch (updateMenuChoice) {
                case 1 -> Driver.performFieldUpdate(connection, "driver_name", newValueOfField, driverId);
                case 2 -> Driver.performFieldUpdate(connection, "driver_city", newValueOfField, driverId);
            }
        }
    }

}