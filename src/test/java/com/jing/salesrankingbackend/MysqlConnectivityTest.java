package com.jing.salesrankingbackend;

import com.jing.salesrankingbackend.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@IntegrationTest
class MysqlConnectivityTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldConnectToMysql() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            assertTrue(resultSet.next(), "MySQL 查询未返回结果");
            assertEquals(1, resultSet.getInt(1), "MySQL 连通性校验失败");
        }
    }
}
