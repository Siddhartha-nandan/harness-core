/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.changehandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.changehandlers.helper.ChangeHandlerHelper;
import io.harness.changestreamsframework.ChangeEvent;
import io.harness.changestreamsframework.ChangeType;
import io.harness.persistence.PersistentEntity;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.timescaledb.TimeScaleDBService;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.mongodb.BasicDBObject;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.joor.Reflect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CDStageHelmManifestInfoHandlerTest extends CategoryTest {
  @Mock private TimeScaleDBService timeScaleDBService;
  private final ChangeHandlerHelper helper = new ChangeHandlerHelper();
  private final CDStageHelmManifestInfoHandler handler = new CDStageHelmManifestInfoHandler();
  private AutoCloseable mocks;
  private final ClassLoader classLoader = this.getClass().getClassLoader();
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    Reflect.on(handler).set("timeScaleDBService", timeScaleDBService);
    Reflect.on(handler).set("changeHandlerHelper", helper);
    doReturn(true).when(timeScaleDBService).isValid();
  }

  @After
  public void tearDown() throws Exception {
    if (mocks != null) {
      mocks.close();
    }
  }

  @Test
  @Owner(developers = OwnerRule.RISHABH)
  @Category(UnitTests.class)
  public void testInsert() throws IOException, SQLException {
    // setup mocks to read final sql query
    Connection mockConnection = mock(Connection.class);
    doReturn(mockConnection).when(timeScaleDBService).getDBConnection();
    doReturn(mock(PreparedStatement.class)).when(mockConnection).prepareStatement(anyString());

    ChangeEvent<PersistentEntity> changeEvent = generateInsertChangeEvent("stage_execution_info.json");

    handler.handleChange(changeEvent, "cd_stage_helm_manifest_info", new String[] {});

    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockConnection, times(0)).prepareStatement(queryCaptor.capture());
  }

  @Test
  @Owner(developers = OwnerRule.RISHABH)
  @Category(UnitTests.class)
  public void testInsertNoHelmManifest() throws IOException, SQLException {
    // setup mocks to read final sql query
    Connection mockConnection = mock(Connection.class);
    doReturn(mockConnection).when(timeScaleDBService).getDBConnection();
    doReturn(mock(PreparedStatement.class)).when(mockConnection).prepareStatement(anyString());

    ChangeEvent<PersistentEntity> changeEvent =
        generateInsertChangeEvent("stage_execution_info_with_no_helm_manifest.json");

    handler.handleChange(changeEvent, "cd_stage_helm_manifest_info", new String[] {});

    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockConnection, times(0)).prepareStatement(queryCaptor.capture());
  }

  @Test
  @Owner(developers = OwnerRule.RISHABH)
  @Category(UnitTests.class)
  public void testInsertSingleHelmManifest() throws IOException, SQLException {
    // setup mocks to read final sql query
    Connection mockConnection = mock(Connection.class);
    doReturn(mockConnection).when(timeScaleDBService).getDBConnection();
    doReturn(mock(PreparedStatement.class)).when(mockConnection).prepareStatement(anyString());

    ChangeEvent<PersistentEntity> changeEvent =
        generateInsertChangeEvent("stage_execution_info_with_one_helm_manifest.json");

    handler.handleChange(changeEvent, "cd_stage_helm_manifest_info", new String[] {});

    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockConnection, times(1)).prepareStatement(queryCaptor.capture());
    final List<String> sqlQueries = queryCaptor.getAllValues();
    assertThat(sqlQueries.get(0))
        .isEqualTo(
            "INSERT INTO cd_stage_helm_manifest_info (stage_execution_id,id,type,chart_version,helm_version) VALUES('WGorBC7tTFStlQshsrAgYA','manifest1','HelmChart','0.1.0','V3') ON CONFLICT (id,stage_execution_id) Do UPDATE  SET stage_execution_id='WGorBC7tTFStlQshsrAgYA',id='manifest1',type='HelmChart',chart_version='0.1.0',helm_version='V3'");
  }

  @Test
  @Owner(developers = OwnerRule.RISHABH)
  @Category(UnitTests.class)
  public void testUpdateMultipleManifest() throws IOException, SQLException {
    // setup mocks to read final sql query
    Connection mockConnection = mock(Connection.class);
    doReturn(mockConnection).when(timeScaleDBService).getDBConnection();
    doReturn(mock(PreparedStatement.class)).when(mockConnection).prepareStatement(anyString());

    ChangeEvent<PersistentEntity> changeEvent =
        generateUpdateChangeEvent("stage_execution_info_with_multiple_manifest.json");

    handler.handleChange(changeEvent, "cd_stage_helm_manifest_info", new String[] {});

    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockConnection, times(2)).prepareStatement(queryCaptor.capture());
    final List<String> sqlQueries = queryCaptor.getAllValues();
    assertThat(sqlQueries.get(0))
        .isEqualTo(
            "INSERT INTO cd_stage_helm_manifest_info (stage_execution_id,id,type,chart_version,helm_version) VALUES('WGorBC7tTFStlQshsrAgYA','manifest1','HelmChart','0.1.0','V3') ON CONFLICT (id,stage_execution_id) Do UPDATE  SET stage_execution_id='WGorBC7tTFStlQshsrAgYA',id='manifest1',type='HelmChart',chart_version='0.1.0',helm_version='V3'");
    assertThat(sqlQueries.get(1))
        .isEqualTo(
            "INSERT INTO cd_stage_helm_manifest_info (stage_execution_id,id,type,chart_version,helm_version) VALUES('WGorBC7tTFStlQshsrAgYA','manifest2','HelmChart','0.2.0','V2') ON CONFLICT (id,stage_execution_id) Do UPDATE  SET stage_execution_id='WGorBC7tTFStlQshsrAgYA',id='manifest2',type='HelmChart',chart_version='0.2.0',helm_version='V2'");
  }

  private ChangeEvent<PersistentEntity> generateInsertChangeEvent(String fileName) throws IOException {
    String s = readFile(fileName);
    return ChangeEvent.builder()
        .uuid("uuid")
        .changeType(ChangeType.INSERT)
        .fullDocument(BasicDBObject.parse(s))
        .build();
  }

  private ChangeEvent<PersistentEntity> generateUpdateChangeEvent(String fileName) throws IOException {
    String s = readFile(fileName);
    return ChangeEvent.builder()
        .uuid("uuid")
        .changeType(ChangeType.UPDATE)
        .fullDocument(BasicDBObject.parse(s))
        .build();
  }

  private String readFile(String fileName) throws IOException {
    final URL testFile = classLoader.getResource(fileName);
    assert testFile != null;
    return Resources.toString(testFile, Charsets.UTF_8);
  }
}
