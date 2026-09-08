package com.allinone.collect.service.impl;

import com.allinone.collect.domain.CollectTemplate;
import com.allinone.collect.mapper.CollectTemplateMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class CollectTemplateVersionIntegrationTest {
    @ParameterizedTest
    @ValueSource(strings = {"allinone_biz.sql", "allinone_biz_update.sql"})
    void queriesActualInstallAndUpgradeSnapshotSchemaAndRejectsOverwrite(String script) throws Exception {
        String source = Files.readString(Path.of("..", "sql", script));
        var matcher = java.util.regex.Pattern.compile("(?is)CREATE TABLE(?: IF NOT EXISTS)? `?collect_template_version`?\\s*\\(.*?;").matcher(source);
        assertThat(matcher.find()).isTrue();
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (var c = ds.getConnection(); var sql = c.createStatement()) { sql.execute(matcher.group()); }
        Configuration config = new Configuration(new Environment("test", new JdbcTransactionFactory(), ds));
        config.getTypeAliasRegistry().registerAlias("CollectTemplate", CollectTemplate.class);
        String resource = "mapper/collect/CollectTemplateMapper.xml";
        try (var xml = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(xml, config, resource, config.getSqlFragments()).parse();
        }
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            var mapper = session.getMapper(CollectTemplateMapper.class);
            assertThat(mapper.insertTemplateVersion(1L, 2, "[{\"name\":\"旧表\"}]", "旧模板", "1", "alice", new Date())).isEqualTo(1);
            session.commit();
            mapper.insertTemplateVersion(1L, 3, "[]", "新模板", "1", "bob", new Date());
            session.commit();
            var old = mapper.selectTemplateVersion(1L, 2);
            assertThat(old.getTemplateName()).isEqualTo("旧模板");
            assertThat(old.getTemplateJson()).contains("旧表");
            assertThat(old.getCreateBy()).isEqualTo("alice");
            assertThat(mapper.selectTemplateVersion(1L, 99)).isNull();
            assertThatThrownBy(() -> mapper.insertTemplateVersion(1L, 2, "[]", "覆盖", "1", "bob", new Date())).isInstanceOf(Exception.class);
            session.rollback(); session.clearCache();
            assertThat(mapper.selectTemplateVersion(1L, 2).getTemplateName()).isEqualTo("旧模板");
        }
    }
}
