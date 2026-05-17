package smartgrid;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    
    // Oracle connection details
    private static final String URL = "jdbc:oracle:thin:@localhost:1521/xepdb1";
    private static final String USERNAME = "System";
    private static final String PASSWORD = "1234";
    
    private static Connection connection = null;
    
    // Method to get connection
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("oracle.jdbc.driver.OracleDriver");
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("✅ Oracle Database Connected Successfully!");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Oracle JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("❌ Connection Failed!");
            e.printStackTrace();
        }
        return connection;
    }
    
    // Method to close connection
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("🔌 Database Connection Closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Main method - YAHI SE RUN KARO
    public static void main(String[] args) {
        System.out.println("Testing Oracle Database Connection...");
        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            System.out.println("🎉 SUCCESS! Smart Grid Database Connected!");
            DBConnection.closeConnection();
        } else {
            System.out.println("❌ FAILED! Could not connect to database.");
        }
    }
}