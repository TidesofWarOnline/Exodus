package com.tidesofwaronline.Exodus.Config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tidesofwaronline.Exodus.Exodus;
import com.tidesofwaronline.Exodus.Items.CustomItem;
import com.tidesofwaronline.Exodus.Items.CustomItemHandler;

public class XMLLoader {

	UUID weaponsUUID = UUID.fromString("bb15a547-acae-4482-9c5f-a45515610466");

	private static HashMap<String, String> enchantments = new HashMap<String, String>();
	private static HashMap<String, String> Vanilla_Items = new HashMap<String, String>();
	private static HashMap<String, HashMap<String, String>> enums = new HashMap<String, HashMap<String, String>>();
	private static File fXmlFile;

	public XMLLoader() {
		parse();

		Thread updater = new XMLUpdater();
		updater.start();
	}

	public static void parse() {

		long time = System.currentTimeMillis();

		try {

			fXmlFile = new File("plugins/Exodus/Exodus.xml");
			if (!fXmlFile.exists()) {
				fXmlFile = new File("E:\\Documents\\Exodus.xml");
			}
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			//optional, but recommended
			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			NodeList enumList = doc
					.getElementsByTagName("EnumerationPropertyDefinition");

			for (int temp = 0; temp < enumList.getLength(); temp++) {
				Node nNode = enumList.item(temp);

				{ //Parse Enumerators 
					if (nNode.getAttributes().getNamedItem("BasedOn") != null) {
						if (nNode
								.getAttributes()
								.getNamedItem("BasedOn")
								.getNodeValue()
								.equalsIgnoreCase(
										"6aaefa6f-e7d8-4a57-94ab-d742f04c126f")) {
							parseEnum((Element) nNode);
						}
					}
				}
			}

			Exodus.logger.info("Parsed " + enums.size() + " enums.");

			NodeList entityList = doc.getElementsByTagName("Entity");

			int itemCount = 0;

			for (int temp = 0; temp < entityList.getLength(); temp++) {

				Node nNode = entityList.item(temp);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					if (eElement.getAttribute("ObjectTemplateReferenceName")
							.equalsIgnoreCase("Enchantment")) {
						parseEnchantment(eElement);
					}

					if (eElement.getAttribute("ObjectTemplateReferenceName")
							.equalsIgnoreCase("Vanilla_Item")) {
						parseVanillaItem(eElement);
					}
				}
			}

			Exodus.logger.info("Parsed " + enchantments.size()
					+ " enchantments.");

			for (int temp = 0; temp < entityList.getLength(); temp++) {

				Node nNode = entityList.item(temp);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					if (eElement.getAttribute("ObjectTemplateReferenceName")
							.equalsIgnoreCase("Custom_Item")) {
						parseItem(eElement);
						itemCount++;
					}
				}
			}

			Exodus.logger.info("Parsed " + itemCount + " items.");

			Exodus.logger.info("Total time: "
					+ (System.currentTimeMillis() - time) + "ms");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void parseVanillaItem(Element eElement) {
		String name = eElement.getElementsByTagName("ExternalId").item(0)
				.getTextContent().trim();
		String uuid = eElement.getAttribute("Guid");
		Vanilla_Items.put(uuid, name);
	}

	private static void parseEnchantment(Element eElement) {
		String name = eElement.getElementsByTagName("ExternalId").item(0)
				.getTextContent().trim();
		String uuid = eElement.getAttribute("Guid");
		enchantments.put(uuid, name);
	}

	private static String getEnchantmentByUUID(String uuid) {
		return enchantments.get(uuid);
	}

	private static String getVanillaItemByUUID(String uuid) {
		return Vanilla_Items.get(uuid);
	}

	private static void parseEnum(Element eElement) {
		String displayName = eElement.getElementsByTagName("TechnicalName")
				.item(0).getTextContent().trim();
		HashMap<String, String> map = new HashMap<String, String>();

		Element e = (Element) eElement.getElementsByTagName("Values").item(0);

		NodeList Value = e.getElementsByTagName("Value");
		NodeList TechnicalName = e.getElementsByTagName("TechnicalName");

		for (int i = 0; i < Value.getLength(); i++) {
			map.put(Value.item(i).getTextContent(), TechnicalName.item(i)
					.getTextContent().toUpperCase());
		}
		enums.put(displayName, map);

	}

	private static String getEnumValue(String map, String key) {
		HashMap<String, String> enumsmap = enums.get(map);
		if (enumsmap == null)
			return null;
		return enumsmap.get(key);
	}

	private static void parseItem(Element eElement) {

		Map<String, Object> itemMap = new HashMap<String, Object>();

		UUID id = UUID.fromString(eElement.getAttribute("Guid"));
		itemMap.put("ID", id);

		String name = eElement.getElementsByTagName("DisplayName").item(0)
				.getTextContent().trim();
		itemMap.put("Name", name);

		NodeList properties = eElement.getElementsByTagName("Properties")
				.item(0).getChildNodes();
		for (int i = 0; i < properties.getLength(); i++) {
			if (properties.item(i).getNodeType() != 3) {

				Node node = properties.item(i);
				String nodeName = node.getAttributes().getNamedItem("Name")
						.getNodeValue();
				String nodeText = node.getTextContent().trim();

				if (node.getNodeName().equals("String")) {
					itemMap.put(nodeName, nodeText);
				}

				if (node.getNodeName().equals("Number")) {
					if (!nodeText.isEmpty()) {
						itemMap.put(nodeName, Integer.parseInt(nodeText));
					}
				}

				if (node.getNodeName().equals("References")) {

					int y = 0;

					for (int x = 0; x < node.getChildNodes().getLength(); x++) {
						if (node.getChildNodes().item(x).hasAttributes()) {
							itemMap.put(nodeName + "." + y,
									getEnchantmentByUUID(node.getChildNodes()
											.item(x).getAttributes()
											.getNamedItem("GuidRef")
											.getNodeValue().trim()));
							y++;
						}
					}
				}

				if (node.getNodeName().equals("NamedReference")) {
					itemMap.put(nodeName, getVanillaItemByUUID(node
							.getAttributes().getNamedItem("GuidRef")
							.getNodeValue().trim()));

				}

				if (node.getNodeName().equals("Boolean")) {
					itemMap.put(nodeName, Boolean.parseBoolean(nodeText
							.replace("0", "False").replace("1", "True")));
				}

				if (node.getNodeName().equals("Enum")) {
					String map = nodeName;
					String key = nodeText;
					if (getEnumValue(map, key) != null) {
						itemMap.put(map, getEnumValue(map, key));
					}
				}

			}
		}

		CustomItem i = new CustomItem(itemMap);
		CustomItemHandler.addDefinedItem(i);
	}

	public static File getFile() {
		return fXmlFile;
	}
}
