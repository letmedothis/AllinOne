package com.allinone.supply.service;

import com.allinone.common.exception.ServiceException;
import com.allinone.supply.domain.ParsedInvoice;
import com.allinone.supply.domain.ParsedInvoiceLine;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 数电票(蓝字)XML 解析 —— 已按脱敏真实票样校准
 * (测试夹具 allinone-supply/src/test/resources/invoices/digital-invoice-01.fixture.xml)。
 *
 * 结构(名称均按 local-name 匹配,兼容命名空间前缀):
 * <pre>
 * EInvoice
 * ├ Header: EIid(20位), InherentLabel/GeneralOrSpecialVAT/LabelName(发票类型) …
 * ├ EInvoiceData
 * │   ├ SellerInformation: SellerIdNum/SellerName …
 * │   ├ BuyerInformation:  BuyerIdNum/BuyerName …
 * │   ├ BasicInformation:  TotalAmWithoutTax / TotalTaxAm / TotalTax-includedAmount / RequestTime
 * │   └ IssuItemInformation+(多行): ItemName/SpecMod/MeaUnits/Quantity/UnPrice/Amount/TaxRate/ComTaxAm …
 * └ TaxSupervisionInfo: InvoiceNumber(20位)/IssueTime(YYYY-MM-DD)
 * </pre>
 * 解析结果仅作回填与人工核对(契约 doc/02-设计与交付/供应链专项/M3_数电票解析_契约.md),不代表税务验真。
 */
public final class DigitalInvoiceXmlParser {

    private DigitalInvoiceXmlParser() {
    }

    public static ParsedInvoice parse(Document doc) {
        Element root = doc.getDocumentElement();
        String rootName = root.getLocalName() == null ? root.getNodeName() : root.getLocalName();
        if (!"EInvoice".equals(rootName)) throw new ServiceException("无法识别的数电票 XML 格式，请上传 EInvoice 蓝字票据");
        ParsedInvoice result = new ParsedInvoice();
        String number = DigitInvoiceNumbers.normalize(firstText(doc, "InvoiceNumber"));
        DigitInvoiceNumbers.require20Digits(number, "发票号码");
        result.setInvoiceNumber(number);
        result.setInvoiceType(resolveInvoiceType(doc));
        result.setIssueDate(resolveIssueDate(doc));

        result.setSellerName(firstText(doc, "SellerName"));
        result.setSellerTaxId(firstText(doc, "SellerIdNum"));
        result.setBuyerName(firstText(doc, "BuyerName"));
        result.setBuyerTaxId(firstText(doc, "BuyerIdNum"));
        result.setAmount(firstText(doc, "TotalAmWithoutTax"));
        result.setTaxAmount(firstText(doc, "TotalTaxAm"));
        result.setTotalAmount(firstText(doc, "TotalTax-includedAmount"));

        List<Element> items = elementsByLocalName(root, "IssuItemInformation");
        if (items.isEmpty()) throw new ServiceException("XML 中没有开票明细(IssuItemInformation)");
        for (Element item : items) {
            ParsedInvoiceLine line = new ParsedInvoiceLine();
            line.setName(textIn(item, "ItemName"));
            line.setSpecification(textIn(item, "SpecMod"));
            line.setUnit(textIn(item, "MeaUnits"));
            line.setQuantity(textIn(item, "Quantity"));
            line.setUnitPrice(textIn(item, "UnPrice"));
            line.setTaxRate(textIn(item, "TaxRate"));
            line.setAmount(textIn(item, "Amount"));
            line.setTaxAmount(textIn(item, "ComTaxAm"));
            result.getLines().add(line);
        }

        require(result.getInvoiceType(), "发票类型");
        require(result.getIssueDate(), "开票日期");
        require(result.getSellerName(), "销售方名称");
        require(result.getSellerTaxId(), "销售方统一社会信用代码");
        require(result.getBuyerName(), "购买方名称");
        require(result.getBuyerTaxId(), "购买方统一社会信用代码");
        for (ParsedInvoiceLine line : result.getLines()) {
            require(line.getName(), "明细/商品名称");
            require(line.getUnit(), "明细/单位");
            require(line.getQuantity(), "明细/数量");
            require(line.getUnitPrice(), "明细/单价");
            require(line.getTaxRate(), "明细/税率");
        }
        return result;
    }

    /** 发票类型:取 GeneralOrSpecialVAT(专/普)标签,缺失回退 EInvoiceType,再缺省"数电票"。 */
    private static String resolveInvoiceType(Document doc) {
        Element general = firstByLocalName(doc, "GeneralOrSpecialVAT");
        String value = general == null ? null : textIn(general, "LabelName");
        if (value == null || value.isBlank()) {
            Element type = firstByLocalName(doc, "EInvoiceType");
            value = type == null ? null : textIn(type, "LabelName");
        }
        return (value == null || value.isBlank()) ? "数电票" : value.trim();
    }

    private static String resolveIssueDate(Document doc) {
        String issueTime = firstText(doc, "IssueTime");
        if (issueTime != null && !issueTime.isBlank()) return issueTime.trim();
        String requestTime = firstText(doc, "RequestTime");
        return requestTime == null || requestTime.length() < 10 ? requestTime : requestTime.substring(0, 10);
    }

    // ---- 按 local-name 查找的通用工具(容忍命名空间前缀) ----

    private static String firstText(Document doc, String localName) {
        Element element = firstByLocalName(doc, localName);
        return element == null ? null : element.getTextContent().trim();
    }

    private static Element firstByLocalName(Document doc, String localName) {
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node node = all.item(i);
            if (node instanceof Element element && localName(element).equals(localName)) return element;
        }
        return null;
    }

    private static List<Element> elementsByLocalName(Element root, String localName) {
        List<Element> found = new ArrayList<>();
        NodeList all = root.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node node = all.item(i);
            if (node instanceof Element element && localName(element).equals(localName)) found.add(element);
        }
        return found;
    }

    private static String textIn(Element parent, String localName) {
        NodeList all = parent.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node node = all.item(i);
            if (node instanceof Element element && localName(element).equals(localName)) return element.getTextContent().trim();
        }
        return null;
    }

    private static String localName(Element element) {
        String tag = element.getTagName();
        int idx = tag.indexOf(':');
        return idx >= 0 ? tag.substring(idx + 1) : tag;
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) throw new ServiceException("数电票 XML 字段不能为空：" + field);
    }
}
