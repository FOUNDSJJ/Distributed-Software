package Database;

import java.nio.file.*;
import java.sql.*;
import java.util.stream.Collectors;

class RunCreateUsersTable {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/Distributed-Software?useSSL=false&serverTimezone=UTC";
        String user = "root";
        String password = "//Sjj20051206";
        String sqlFilePath = "D:\\Private\\Grade_3\\Distributed-Software-HW\\Back_End\\Database\\create_users_table.sql"; // SQL 文件位于当前模块目录中

        try {
            // 加载 MySQL JDBC 驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 读取 SQL 文件内容
            String sql = Files.lines(Paths.get(sqlFilePath))
                    .map(String::trim)
                    .filter(line -> !line.startsWith("--") && !line.isEmpty()) // 过滤注释和空行
                    .collect(Collectors.joining(" "));

            // 按分号拆分多条 SQL 语句并依次执行
            String[] statements = sql.split(";");

            try (Connection conn = DriverManager.getConnection(url, user, password);
                 Statement stmt = conn.createStatement()) {

                for (String s : statements) {
                    if (!s.trim().isEmpty()) {
                        stmt.execute(s);
                    }
                }

                System.out.println("表创建完成，数据插入成功！");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
