package com.allinone.supply.service;

import com.allinone.common.exception.ServiceException;
import com.allinone.supply.domain.ParsedInvoice;
import com.allinone.supply.domain.ParsedInvoiceLine;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** 仅解析约定 XML，不兼容真实税务 XML；默认关闭 DTD、外部实体和外部资源。 */
public class InvoiceXmlParser {
    public ParsedInvoice parse(String xml) {
        if (xml == null || xml.length() > 2_000_000) throw new ServiceException("XML 为空或超过 2MB 限制");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(false);
            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            if (root == null || !"DemoInvoice".equals(root.getTagName()) || !"1.0".equals(root.getAttribute("schemaVersion"))) throw new ServiceException("仅支持 DemoInvoice schemaVersion=1.0");
            ParsedInvoice result = new ParsedInvoice(); result.setInvoiceNumber(text(root, "InvoiceNumber")); result.setInvoiceType(text(root, "InvoiceType")); result.setIssueDate(text(root, "IssueDate"));
            Element seller = section(root, "Seller"); Element buyer = section(root, "Buyer"); Element totals = section(root, "Totals");
            if (seller == null || buyer == null || totals == null) throw new ServiceException("XML 缺少 Seller、Buyer 或 Totals 节点");
            result.setSellerName(text(seller, "Name")); result.setSellerTaxId(text(seller, "TaxId")); result.setBuyerName(text(buyer, "Name")); result.setBuyerTaxId(text(buyer, "TaxId"));
            result.setAmount(text(totals, "Amount")); result.setTaxAmount(text(totals, "TaxAmount")); result.setTotalAmount(text(totals, "TotalAmount"));
            var lines = root.getElementsByTagName("Line"); for (int i = 0; i < lines.getLength(); i++) { Element line = (Element) lines.item(i); ParsedInvoiceLine item = new ParsedInvoiceLine(); item.setName(text(line, "Name")); item.setSpecification(text(line, "Specification")); item.setUnit(text(line, "Unit")); item.setQuantity(text(line, "Quantity")); item.setUnitPrice(text(line, "UnitPrice")); item.setTaxRate(text(line, "TaxRate")); item.setAmount(text(line, "Amount")); item.setTaxAmount(text(line, "TaxAmount")); result.getLines().add(item); }
            if (result.getLines().isEmpty()) throw new ServiceException("XML 中没有发票明细");
            require(result.getInvoiceNumber(), "InvoiceNumber"); require(result.getInvoiceType(), "InvoiceType"); require(result.getIssueDate(), "IssueDate");
            require(result.getSellerName(), "Seller/Name"); require(result.getSellerTaxId(), "Seller/TaxId"); require(result.getBuyerName(), "Buyer/Name"); require(result.getBuyerTaxId(), "Buyer/TaxId");
            for (ParsedInvoiceLine line : result.getLines()) { require(line.getName(), "Line/Name"); require(line.getUnit(), "Line/Unit"); require(line.getQuantity(), "Line/Quantity"); require(line.getUnitPrice(), "Line/UnitPrice"); require(line.getTaxRate(), "Line/TaxRate"); }
            return result;
        } catch (ServiceException e) { throw e; } catch (Exception e) { throw new ServiceException("XML 解析失败，请确认文件为 DemoInvoice v1 格式"); }
    }
    private String text(Element root, String tag) { return text(root, tag, 0); }
    private String text(Element root, String tag, int index) { var nodes = root.getElementsByTagName(tag); if (index >= nodes.getLength()) return null; return nodes.item(index).getTextContent().trim(); }
    private Element section(Element root, String tag) { var nodes = root.getElementsByTagName(tag); return nodes.getLength() == 0 ? null : (Element) nodes.item(0); }
    private void require(String value, String field) { if (value == null || value.isBlank()) throw new ServiceException("XML 字段不能为空：" + field); }
}
