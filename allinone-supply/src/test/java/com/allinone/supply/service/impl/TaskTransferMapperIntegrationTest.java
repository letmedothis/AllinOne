package com.allinone.supply.service.impl;

import com.allinone.supply.mapper.TaskTransferMapper;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TaskTransferMapperIntegrationTest {
    @Test void enforcesActiveRolesPermissionsScopeAndCurrentRoundSeparation() throws Exception {
        JdbcDataSource ds = new JdbcDataSource(); ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (var c = ds.getConnection(); var s = c.createStatement()) {
            s.execute("create table sys_user(user_id bigint,dept_id bigint,rank_level varchar(20),status char,del_flag char)");
            s.execute("create table sys_role(role_id bigint,role_key varchar(30),status char,del_flag char)");
            s.execute("create table sys_user_role(user_id bigint,role_id bigint)");
            s.execute("create table sys_role_menu(role_id bigint,menu_id bigint)");
            s.execute("create table sys_menu(menu_id bigint,status char,perms varchar(80))");
            s.execute("create table documents(id bigint,creator_id bigint,current_version int,approval_status varchar(20),deleted char)");
            s.execute("create table document_versions(id bigint,document_id bigint,version_no int,submitted_by bigint)");
            s.execute("create table workflow_instances(id bigint,version_id bigint)");
            s.execute("create table workflow_tasks(instance_id bigint,status varchar(20),acted_by bigint)");
            s.execute("insert into sys_user values(10,1,'STAFF','0','0'),(20,1,'LEADER','0','0'),(30,2,'LEADER','0','0'),(40,2,'EXEC','0','0'),(50,1,'LEADER','1','0')");
            s.execute("insert into sys_role values(1,'supervisor','0','0'),(2,'finance','1','0')");
            s.execute("insert into sys_user_role values(20,1),(30,2),(50,1)");
            s.execute("insert into sys_menu values(1,'0','supply:approval:approve')");
            s.execute("insert into sys_role_menu values(1,1),(2,1)");
            s.execute("insert into documents values(100,10,2,'IN_REVIEW','0')");
            s.execute("insert into document_versions values(101,100,1,10),(102,100,2,10)");
            s.execute("insert into workflow_instances values(1,101),(2,102)");
            s.execute("insert into workflow_tasks values(1,'COMPLETED',40),(2,'COMPLETED',20)");
        }
        Configuration config = new Configuration(new Environment("test", new JdbcTransactionFactory(), ds));
        for (String resource : new String[]{"mapper/supply/TaskTransferMapper.xml", "mapper/supply/WorkflowEngineMapper.xml", "mapper/supply/ApprovalMapper.xml"}) {
            try (var xml = getClass().getClassLoader().getResourceAsStream(resource)) { new XMLMapperBuilder(xml, config, resource, config.getSqlFragments()).parse(); }
        }
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            var m = session.getMapper(TaskTransferMapper.class);
            assertThat(m.countScope(100L,20L)).isEqualTo(1);
            assertThat(m.countScope(100L,30L)).isZero();
            assertThat(m.countScope(100L,40L)).isEqualTo(1);
            assertThat(m.countScope(100L,50L)).isZero();
            assertThat(m.countRole(20L,"supervisor")).isEqualTo(1);
            assertThat(m.countRole(30L,"finance")).isZero();
            assertThat(m.countPermission(20L,"supply:approval:approve")).isEqualTo(1);
            assertThat(m.countPermission(30L,"supply:approval:approve")).isZero();
            assertThat(m.countPermission(50L,"supply:approval:approve")).isZero();
            assertThat(m.countConflict(100L,10L)).isEqualTo(1);
            assertThat(m.countConflict(100L,20L)).isEqualTo(1);
            assertThat(m.countConflict(100L,40L)).isZero();
            assertThat(m.lockDocument(100L)).isEqualTo(100L);
        }
    }
}
