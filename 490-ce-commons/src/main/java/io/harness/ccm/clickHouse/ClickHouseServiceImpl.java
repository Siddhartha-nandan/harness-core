/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.clickHouse;

import io.harness.ccm.commons.beans.config.ClickHouseConfig;

import com.clickhouse.jdbc.ClickHouseDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClickHouseServiceImpl implements ClickHouseService {
  @Override
  public Connection getConnection(ClickHouseConfig clickHouseConfig) throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("ssl", "true");
    properties.setProperty("sslmode", "NONE");
    return getConnection(clickHouseConfig, properties);
  }

  @Override
  public Connection getConnection(ClickHouseConfig clickHouseConfig, Properties properties) throws SQLException {
    properties.setProperty("ssl", "true");
    properties.setProperty("sslmode", "NONE");
    ClickHouseDataSource dataSource = new ClickHouseDataSource(clickHouseConfig.getUrl(), properties);
    return dataSource.getConnection(clickHouseConfig.getUsername(), clickHouseConfig.getPassword());
  }

  @Override
  public ResultSet getQueryResult(ClickHouseConfig clickHouseConfig, String query) throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("ssl", "true");
    properties.setProperty("sslmode", "NONE");
    ClickHouseDataSource dataSource = new ClickHouseDataSource(clickHouseConfig.getUrl(), properties);
    Connection connection = dataSource.getConnection(clickHouseConfig.getUsername(), clickHouseConfig.getPassword());
    try (Statement statement = connection.createStatement()) {
      try (ResultSet resultSet = statement.executeQuery(query)) {
        return resultSet;
      }
    }
  }

  @Override
  public List<String> executeClickHouseQuery(ClickHouseConfig clickHouseConfig, String query, boolean returnResult)
      throws SQLException {
    log.info(query);
    String url = clickHouseConfig.getUrl();
    Properties properties = new Properties();
    properties.put("socket_timeout", clickHouseConfig.getSocketTimeout());
    properties.setProperty("ssl", "true");
    properties.setProperty("sslmode", "NONE");
    ClickHouseDataSource dataSource = new ClickHouseDataSource(url, properties);
    try (Connection connection =
             dataSource.getConnection(clickHouseConfig.getUsername(), clickHouseConfig.getPassword());
         Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {
      if (returnResult) {
        List<String> output = new ArrayList<>();
        while (resultSet.next()) {
          output.add(resultSet.getString(1));
        }
        return output;
      }
    }
    return null;
  }
}