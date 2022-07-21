import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Item {

    //Attributes
    String itemName;
    float price;

    // Global variables

    public static int rowsAffected;

    //Methods

    /**
     * The constructor for the item class. It is not used but has been included for the sake of completeness.
     *
     * @param itemName The description of the menu item.
     * @param price The price of the item.
     */
    public Item(String itemName, float price) {
        this.itemName = itemName;
        this.price = price;
    }

    /**
     * This method handles the creation of new menu items.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param input The Scanner instance from the <code>main</code> method needed to read user input with the help of
     *             the <code>UserInput</code> utility class.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void createNewItem(Connection connection, Scanner input) throws SQLException {

        System.out.println("Add New Menu Item\n");

        /* User input of the item's details.*/
        String itemName = UserInput.readString("Item Name: ", input);
        double itemPrice = UserInput.readDouble("Item Price: ", input);

        /* Using PreparedStatement to insert the variables into the item table.*/
        String mySQLQueryCreateNewItem = "INSERT INTO item VALUES (?, ?, ?)";
        PreparedStatement pstmtCreateNewItem = connection.prepareStatement(mySQLQueryCreateNewItem);
        pstmtCreateNewItem.setString(1, null); //Auto_increment
        pstmtCreateNewItem.setString(2, itemName);
        pstmtCreateNewItem.setDouble(3, itemPrice);

        /* Execution of statement and determining if the addition was successful (with user feedback).*/
        rowsAffected = pstmtCreateNewItem.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("The item was successfully created.\n");
        } else {
            System.out.println("The item could not be created. Please review your input\n");
        }

        /* Closing of resource to prevent resource leaking. */
        pstmtCreateNewItem.close();

    }


    /**
     * A method to find an items name from its unique item id. This method is used in the display and invoice
     * functionalities.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here by the
     *                   <code>PreparedStatement</code>.
     * @param itemId A unique identification number for a menu item.
     * @return The name of the item identified by the itemId.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static String findItemName(Connection connection, int itemId) throws SQLException {

        /*Declaration and initialisation of variable because it is needed outside the while loop.*/
        String itemName = "";

        /* Using a PreparedStatement to select the item name based on a unique item ID number.*/
        String mySQLQueryFindItem = "SELECT item_name FROM item WHERE item_id = ?;";
        PreparedStatement pstmtFindItem = connection.prepareStatement(mySQLQueryFindItem);
        pstmtFindItem.setInt(1, itemId);

        /*
         * Execution of statement and return of the method as a ResultSet. The getter method of the ResultSet is used
         * to get the value of the itemName.
         */
        ResultSet resultsFindItem = pstmtFindItem.executeQuery();
        while (resultsFindItem.next()) {
            itemName = resultsFindItem.getString("item_name");
        }

        /* Closing resources to prevent resource leaking */
        pstmtFindItem.close();
        resultsFindItem.close();

        return itemName;
    }

    /**
     * A method to find an item's price from its unique item id. It is used as a utility in the <code>ItemsOrder
     * .addItemToOrder</code> method as well as the display and print invoice functionalities of the program that
     * resides as utilities in the Order class.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param itemId The unique id number for the menu item.
     * @return The price of the item based on its unique id number.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static double findItemPrice(Connection connection, int itemId) throws SQLException {

        /* Declaration and initialisation of this variable because it is needed outside the while loop.*/
        double itemPrice = 0.00;

        /*Use of a PreparedStatement to select the item_price field for a specific item ID.*/
        String mySQLQueryFindPrice = "SELECT item_price FROM item WHERE item_id = ?;";
        PreparedStatement pstmtFindPrice = connection.prepareStatement(mySQLQueryFindPrice);
        pstmtFindPrice.setInt(1, itemId);

        /*
         * Execution of the statement with a return of a ResultSet. The getter method of this ResultSet is used to
         * obtain the itemPrice.
         */
        ResultSet resultsFindPrice = pstmtFindPrice.executeQuery();
        while (resultsFindPrice.next()) {
            itemPrice = resultsFindPrice.getDouble("item_price");
        }

        /* Closing resources to prevent resource leaking.*/
        pstmtFindPrice.close();
        resultsFindPrice.close();

        return itemPrice;

    }

    /**
     * A generic method to update any field in the <code>item</code> table. This method is used as a utility in the
     * <code>performFieldUpdate</code> method in the main file of the program. This method is invoked from the
     * <code>main</code> method.
     *
     * @param connection The Connection resource from the <code>main</code> method needed here for the
     *                   <code>PreparedStatement</code>.
     * @param fieldToUpdate The name in string format of the field that must be updated.
     * @param newValue The new string value of the field (if applicable). The data type depends on the field and
     *                 there is an if/else statement to handle different data types differently.
     * @param newValueDouble The new double value of the field (if applicable). The same handling applies as with
     *                       <code>newValue</code> above.
     * @param typeOfData The type of data of the new value to enable different handling.
     * @param itemId The unique id number of the menu item.
     * @throws SQLException If the underlying MySQL service fails.
     */
    public static void performFieldUpdate (Connection connection, String fieldToUpdate, String newValue,
                                                 double newValueDouble, String typeOfData,
                                                 int itemId) throws SQLException {

        /*
         * Setting up two different MySQL statements because this method can either require a String or a double data
         * type depending on which value needs to be updated. The if/else statement uses the typeOfData parameter to
         * determine which MySQL to use.
         */
        String mySQLQueryFieldUpdate = "";
        if (typeOfData.equalsIgnoreCase("string")) {
            mySQLQueryFieldUpdate =
                    "UPDATE item SET " + fieldToUpdate  + " = '" + newValue + "' WHERE item_id = ?;";
        } else if (typeOfData.equalsIgnoreCase("double")) {
            mySQLQueryFieldUpdate =
                    "UPDATE item SET " + fieldToUpdate  + " = '" + newValueDouble + "' WHERE item_id = ?;";
        }

        /* The PreparedStatement and insertion of the variable as normal. */
        PreparedStatement pstmtFieldUpdate = connection.prepareStatement(mySQLQueryFieldUpdate);
        pstmtFieldUpdate.setInt(1, itemId);

        /* Execution of the statement and determination of whether the update was successful or not based on the
        return of the method. */
        rowsAffected = pstmtFieldUpdate.executeUpdate();
        if (rowsAffected > 0) {
            System.out.println("The item was updated successfully.\n");
        } else {
            System.out.println("The update could not be done. Please check that the customer ID is correct.\n");
        }

        /* Closing resource to prevent resource leaking.*/
        pstmtFieldUpdate.close();

    }
}