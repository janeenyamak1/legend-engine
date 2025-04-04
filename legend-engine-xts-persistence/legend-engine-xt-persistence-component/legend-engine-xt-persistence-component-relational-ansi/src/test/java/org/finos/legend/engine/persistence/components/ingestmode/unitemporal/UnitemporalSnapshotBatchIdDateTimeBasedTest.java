// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.persistence.components.ingestmode.unitemporal;

import org.finos.legend.engine.persistence.components.AnsiTestArtifacts;
import org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.unitemporal.UnitmemporalSnapshotBatchIdDateTimeBasedTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.AnsiTestArtifacts.*;
import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.DATA_ERROR_ROWS;

public class UnitemporalSnapshotBatchIdDateTimeBasedTest extends UnitmemporalSnapshotBatchIdDateTimeBasedTestCases
{
    String incomingRecordCount = "SELECT COUNT(*) as \"incomingRecordCount\" FROM \"mydb\".\"staging\" as stage";
    String rowsUpdated = "SELECT COUNT(*) as \"rowsUpdated\" FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'))))";
    String rowsDeleted = "SELECT 0 as \"rowsDeleted\"";
    String rowsInserted = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'))-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'))))) as \"rowsInserted\"";
    String rowsTerminated = "SELECT (SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1)-(SELECT COUNT(*) FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1) AND (EXISTS (SELECT * FROM \"mydb\".\"main\" as sink2 WHERE ((sink2.\"id\" = sink.\"id\") AND (sink2.\"name\" = sink.\"name\")) AND (sink2.\"batch_id_in\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'))))) as \"rowsTerminated\"";

    @Override
    public void verifyUnitemporalSnapshotWithoutPartitionNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1,sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (NOT (EXISTS " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" = stage.\"digest\"))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),999999999,'2000-01-01 00:00:00.000000','9999-12-31 23:59:59' " +
                "FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (stage.\"digest\" IN (SELECT sink.\"digest\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999)))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));

        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalSnapshotWithoutPartitionNoDedupMaxVersion(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> deduplicationAndVersioningSql = operations.deduplicationAndVersioningSql();
        Map<DedupAndVersionErrorSqlType, String> deduplicationAndVersioningErrorChecksSql = operations.deduplicationAndVersioningErrorChecksSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1,sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (NOT (EXISTS " +
                "(SELECT * FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" = stage.\"digest\"))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),999999999,'2000-01-01 00:00:00.000000','9999-12-31 23:59:59' " +
                "FROM \"mydb\".\"staging_temp_staging_lp_yosulf\" as stage " +
                "WHERE NOT (stage.\"digest\" IN (SELECT sink.\"digest\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999)))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTempStagingTablePlusDigest, preActionsSql.get(2));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQuery, deduplicationAndVersioningSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedInsertIntoBaseTempStagingPlusDigestWithMaxVersionAndAllowDuplicates, deduplicationAndVersioningSql.get(1));
        Assertions.assertEquals(getExpectedMaxDataErrorQueryWithDistinctDigest(), deduplicationAndVersioningErrorChecksSql.get(DedupAndVersionErrorSqlType.MAX_DATA_ERRORS));
        Assertions.assertEquals(getExpectedDataErrorQueryWithDistinctDigest(), deduplicationAndVersioningErrorChecksSql.get(DATA_ERROR_ROWS));

        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalSnapshotWithoutPartitionWithDeleteTargetDataEmptyBatchHandling(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1,sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE sink.\"batch_id_out\" = 999999999";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(1));
        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithoutPartitionWithUpperCaseOptimizerFilterDupsMaxVersion(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> deduplicationAndVersioningSql = operations.deduplicationAndVersioningSql();
        Map<DedupAndVersionErrorSqlType, String> deduplicationAndVersioningErrorChecksSql = operations.deduplicationAndVersioningErrorChecksSql();

        String expectedMilestoneQuery = "UPDATE \"MYDB\".\"MAIN\" as sink SET sink.\"BATCH_ID_OUT\" = " +
                "(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE " +
                "UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN')-1,sink.\"BATCH_TIME_OUT\" = '2000-01-01 00:00:00.000000' " +
                "WHERE (sink.\"BATCH_ID_OUT\" = 999999999) AND (NOT (EXISTS (SELECT * FROM \"MYDB\".\"STAGING_TEMP_STAGING_LP_YOSULF\" as stage " +
                "WHERE ((sink.\"ID\" = stage.\"ID\") AND (sink.\"NAME\" = stage.\"NAME\")) AND (sink.\"DIGEST\" = stage.\"DIGEST\"))))";
        String expectedUpsertQuery = "INSERT INTO \"MYDB\".\"MAIN\" " +
                "(\"ID\", \"NAME\", \"AMOUNT\", \"BIZ_DATE\", \"DIGEST\", \"BATCH_ID_IN\", \"BATCH_ID_OUT\", \"BATCH_TIME_IN\", \"BATCH_TIME_OUT\") " +
                "(SELECT stage.\"ID\",stage.\"NAME\",stage.\"AMOUNT\",stage.\"BIZ_DATE\",stage.\"DIGEST\"," +
                "(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA " +
                "WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN'),999999999,'2000-01-01 00:00:00.000000','9999-12-31 23:59:59' FROM \"MYDB\".\"STAGING_TEMP_STAGING_LP_YOSULF\" as stage " +
                "WHERE NOT (stage.\"DIGEST\" IN (SELECT sink.\"DIGEST\" FROM \"MYDB\".\"MAIN\" as sink WHERE sink.\"BATCH_ID_OUT\" = 999999999)))";
        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableCreateQueryWithUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQueryWithUpperCase(), preActionsSql.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedBaseTempStagingTablePlusDigestWithCountUpperCase, preActionsSql.get(2));

        Assertions.assertEquals(AnsiTestArtifacts.expectedTempStagingCleanupQueryInUpperCase, deduplicationAndVersioningSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedInsertIntoBaseTempStagingPlusDigestWithMaxVersionAndFilterDuplicatesUpperCase, deduplicationAndVersioningSql.get(1));
        Assertions.assertEquals(getExpectedMaxDataErrorQueryWithDistinctDigestUpperCase(), deduplicationAndVersioningErrorChecksSql.get(DedupAndVersionErrorSqlType.MAX_DATA_ERRORS));
        Assertions.assertEquals(getExpectedDataErrorQueryWithDistinctDigestUpperCase(), deduplicationAndVersioningErrorChecksSql.get(DATA_ERROR_ROWS));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithAdditionalMetadataWithBatchSuccessValueWithUpperCase(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1,sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (NOT (EXISTS " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" = stage.\"digest\")))) " +
                "AND (EXISTS (SELECT * FROM \"mydb\".\"staging\" as stage WHERE sink.\"biz_date\" = stage.\"biz_date\"))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),999999999,'2000-01-01 00:00:00.000000','9999-12-31 23:59:59' " +
                "FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (stage.\"digest\" IN (SELECT sink.\"digest\" FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = 999999999) AND (sink.\"biz_date\" = stage.\"biz_date\"))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQueryWithAdditionalMetadata(), metadataIngestSql.get(0));

        verifyStats(operations, incomingRecordCount, rowsUpdated, rowsDeleted, rowsInserted, rowsTerminated);
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionWithDefaultEmptyDataHandling(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertTrue(milestoningSql.isEmpty());
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionFiltersNoDedupNoVersioning(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1,sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (NOT (EXISTS " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" = stage.\"digest\")))) " +
                "AND (sink.\"biz_date\" IN ('2000-01-01 00:00:00','2000-01-02 00:00:00'))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),999999999,'2000-01-01 00:00:00.000000','9999-12-31 23:59:59' " +
                "FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (stage.\"digest\" IN (SELECT sink.\"digest\" FROM \"mydb\".\"main\" as sink WHERE (sink.\"batch_id_out\" = 999999999) AND (sink.\"biz_date\" IN ('2000-01-01 00:00:00','2000-01-02 00:00:00')))))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPartitionFiltersWithDeleteTargetDataEmptyDataHandling(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1,sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (sink.\"biz_date\" IN ('2000-01-01 00:00:00','2000-01-02 00:00:00'))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithCleanStagingData(GeneratorResult operations)
    {
        List<String> postActionsSql = operations.postActionsSql();
        Assertions.assertEquals(AnsiTestArtifacts.expectedStagingCleanupQuery, postActionsSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithLessColumnsInStaging(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = (SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN')-1,sink.\"batch_time_out\" = '2000-01-01 00:00:00.000000' " +
                "WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (NOT (EXISTS " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" = stage.\"digest\"))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"digest\"," +
                "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),999999999,'2000-01-01 00:00:00.000000','9999-12-31 23:59:59' " +
                "FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (stage.\"digest\" IN (SELECT sink.\"digest\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999)))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
    }

    @Override
    public void verifyUnitemporalSnapshotWithPlaceholders(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();

        String expectedMilestoneQuery = "UPDATE \"mydb\".\"main\" as sink " +
                "SET sink.\"batch_id_out\" = {BATCH_ID_PATTERN}-1,sink.\"batch_time_out\" = '{BATCH_START_TS_PATTERN}' " +
                "WHERE (sink.\"batch_id_out\" = 999999999) " +
                "AND (NOT (EXISTS " +
                "(SELECT * FROM \"mydb\".\"staging\" as stage WHERE ((sink.\"id\" = stage.\"id\") AND (sink.\"name\" = stage.\"name\")) AND (sink.\"digest\" = stage.\"digest\"))))";

        String expectedUpsertQuery = "INSERT INTO \"mydb\".\"main\" " +
                "(\"id\", \"name\", \"amount\", \"biz_date\", \"digest\", \"batch_id_in\", \"batch_id_out\", \"batch_time_in\", \"batch_time_out\") " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"amount\",stage.\"biz_date\",stage.\"digest\"," +
                "{BATCH_ID_PATTERN},999999999,'{BATCH_START_TS_PATTERN}','9999-12-31 23:59:59' " +
                "FROM \"mydb\".\"staging\" as stage " +
                "WHERE NOT (stage.\"digest\" IN (SELECT sink.\"digest\" FROM \"mydb\".\"main\" as sink WHERE sink.\"batch_id_out\" = 999999999)))";

        Assertions.assertEquals(AnsiTestArtifacts.expectedMainTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(getExpectedMetadataTableCreateQuery(), preActionsSql.get(1));

        Assertions.assertEquals(expectedMilestoneQuery, milestoningSql.get(0));
        Assertions.assertEquals(expectedUpsertQuery, milestoningSql.get(1));
        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableIngestQueryWithPlaceHolders, metadataIngestSql.get(0));
    }

    @Override
    public RelationalSink getRelationalSink()
    {
        return AnsiSqlSink.get();
    }

    protected String getExpectedMetadataTableIngestQuery()
    {
        return AnsiTestArtifacts.expectedMetadataTableIngestQuery;
    }

    protected String getExpectedMetadataTableIngestQueryWithUpperCase()
    {
        return AnsiTestArtifacts.expectedMetadataTableIngestQueryWithUpperCase;
    }

    protected String getExpectedMetadataTableIngestQueryWithAdditionalMetadata()
    {
        return AnsiTestArtifacts.expectedMetadataTableIngestQueryWithAdditionalMetadata;
    }

    protected String getExpectedMetadataTableIngestQueryWithAdditionalMetadataWithUpperCase()
    {
        return AnsiTestArtifacts.expectedMetadataTableIngestQueryWithAdditionalMetadataWithUpperCase;
    }

    protected String getExpectedMetadataTableIngestQueryWithAdditionalMetadataWithBatchSuccessValueWithUpperCase()
    {
        return AnsiTestArtifacts.expectedMetadataTableIngestQueryWithAdditionalMetadataWithBatchSuccessWithUpperCase;
    }

    protected String getExpectedMetadataTableCreateQuery()
    {
        return AnsiTestArtifacts.expectedMetadataTableCreateQuery;
    }

    protected String getExpectedMetadataTableCreateQueryWithUpperCase()
    {
        return AnsiTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase;
    }

    protected String getExpectedMaxDataErrorQueryWithDistinctDigest()
    {
        return AnsiTestArtifacts.dataErrorCheckSqlWithBizDateVersion;
    }

    protected String getExpectedDataErrorQueryWithDistinctDigest()
    {
        return AnsiTestArtifacts.dataErrorsSqlWithBizDateVersion;
    }

    protected String getExpectedMaxDataErrorQueryWithDistinctDigestUpperCase()
    {
        return AnsiTestArtifacts.dataErrorCheckSqlWithBizDateAsVersionUpperCase;
    }

    protected String getExpectedDataErrorQueryWithDistinctDigestUpperCase()
    {
        return AnsiTestArtifacts.dataErrorsSqlWithBizDateVersionUpperCase;
    }
}
