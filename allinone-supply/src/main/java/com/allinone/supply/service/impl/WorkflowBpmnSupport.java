package com.allinone.supply.service.impl;

import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.StringUtils;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** 安全解析并校验设计器输出的 BPMN 及 AllinOne 节点审批属性。 */
final class WorkflowBpmnSupport {
    static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    static final String FLOWABLE_NS = "http://flowable.org/bpmn";
    static final String ALLINONE_NS = "https://allinone.local/workflow";
    static final String TYPE_SUPERVISOR_CONFIG = "SUPERVISOR_CONFIG";
    static final String TYPE_USER = "USER";
    static final String TYPE_ROLE = "ROLE";
    static final String TYPE_DEPT_LEADER = "DEPT_LEADER";
    static final String TYPE_INITIATOR_MANAGER = "INITIATOR_MANAGER";
    private static final Set<String> TYPES = Set.of(TYPE_SUPERVISOR_CONFIG, TYPE_USER, TYPE_ROLE, TYPE_DEPT_LEADER, TYPE_INITIATOR_MANAGER);
    private static final Pattern NODE_ID = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,63}");
    private static final Pattern ROLE_KEY = Pattern.compile("[A-Za-z0-9_-]{1,100}");

    private WorkflowBpmnSupport() { }

    static ParsedBpmn parseAndValidate(String xml, String expectedProcessKey) {
        if (StringUtils.isEmpty(xml)) throw new ServiceException("流程 BPMN 内容不能为空");
        String source = xml.trim();
        String upper = source.toUpperCase(Locale.ROOT);
        if (!source.startsWith("<?xml") || upper.contains("<!DOCTYPE") || upper.contains("<!ENTITY")) {
            throw new ServiceException("流程定义必须是安全的 BPMN XML，且不允许外部实体");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
            NodeList processes = document.getElementsByTagNameNS(BPMN_NS, "process");
            Element process = null;
            for (int i = 0; i < processes.getLength(); i++) {
                Element item = (Element) processes.item(i);
                if (expectedProcessKey.equals(item.getAttribute("id"))) process = item;
            }
            if (process == null) throw new ServiceException("流程定义中未找到流程标识：" + expectedProcessKey);
            String processName = process.getAttribute("name");
            NodeList tasks = process.getElementsByTagNameNS(BPMN_NS, "userTask");
            if (tasks.getLength() == 0) throw new ServiceException("流程至少需要一个人工审批节点");
            List<Assignment> assignments = new ArrayList<>();
            for (int i = 0; i < tasks.getLength(); i++) assignments.add(validateTask((Element) tasks.item(i)));
            return new ParsedBpmn(processName, assignments);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("BPMN XML 无法解析：" + e.getMessage());
        }
    }

    private static Assignment validateTask(Element task) {
        String id = task.getAttribute("id");
        String name = task.getAttribute("name");
        String type = task.getAttributeNS(ALLINONE_NS, "assigneeType").trim().toUpperCase(Locale.ROOT);
        String value = task.getAttributeNS(ALLINONE_NS, "assigneeValue").trim();
        String assignee = task.getAttributeNS(FLOWABLE_NS, "assignee").trim();
        String candidateGroups = task.getAttributeNS(FLOWABLE_NS, "candidateGroups").trim();
        if (StringUtils.isEmpty(type) && "${approverUserId}".equals(assignee)) type = TYPE_SUPERVISOR_CONFIG;
        if (!NODE_ID.matcher(id).matches()) throw new ServiceException("人工节点 ID 不合法：" + id);
        if (StringUtils.isEmpty(name)) throw new ServiceException("人工节点必须填写节点名称：" + id);
        if (!TYPES.contains(type)) throw new ServiceException("请为节点“" + name + "”配置审批人类型");
        String dynamicExpression = "${wfAssignee_" + id + "}";
        switch (type) {
            case TYPE_SUPERVISOR_CONFIG -> require(TYPE_SUPERVISOR_CONFIG, name, value, assignee, candidateGroups, "${approverUserId}");
            case TYPE_USER -> {
                if (!value.matches("\\d{1,20}")) throw new ServiceException("节点“" + name + "”必须选择有效用户");
                require(TYPE_USER, name, value, assignee, candidateGroups, value);
            }
            case TYPE_ROLE -> {
                if (!ROLE_KEY.matcher(value).matches()) throw new ServiceException("节点“" + name + "”必须选择有效角色");
                if (!StringUtils.isEmpty(assignee) || !value.equals(candidateGroups)) throw new ServiceException("节点“" + name + "”的角色候选配置不一致，请重新选择审批角色");
            }
            case TYPE_DEPT_LEADER -> {
                if (!value.matches("\\d{1,20}")) throw new ServiceException("节点“" + name + "”必须选择有效部门");
                require(TYPE_DEPT_LEADER, name, value, assignee, candidateGroups, dynamicExpression);
            }
            case TYPE_INITIATOR_MANAGER -> require(TYPE_INITIATOR_MANAGER, name, value, assignee, candidateGroups, dynamicExpression);
            default -> throw new ServiceException("不支持的审批人类型");
        }
        return new Assignment(id, name, type, value);
    }

    private static void require(String type, String name, String value, String assignee, String groups, String expectedAssignee) {
        if (TYPE_SUPERVISOR_CONFIG.equals(type) || TYPE_INITIATOR_MANAGER.equals(type)) {
            if (!StringUtils.isEmpty(value)) throw new ServiceException("节点“" + name + "”的审批参数应为空");
        }
        if (!expectedAssignee.equals(assignee) || !StringUtils.isEmpty(groups)) {
            throw new ServiceException("节点“" + name + "”的审批人配置不一致，请在右侧属性面板重新选择");
        }
    }

    record Assignment(String nodeId, String nodeName, String type, String value) { }
    record ParsedBpmn(String processName, List<Assignment> assignments) { }
}
