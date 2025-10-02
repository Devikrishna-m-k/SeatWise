import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String DB_FILE = "data/smartexam.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}

