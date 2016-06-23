package modbus;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.util.handler.Handler;
import org.dsa.iot.dslink.util.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EditableFolder {
	private static final Logger LOGGER;

	static {
		LOGGER = LoggerFactory.getLogger(LocalSlaveFolder.class);
	}

	static final String ATTRIBUTE_NAME = "name";
	static final String ATTRIBUTE_OFFSET = "offset";
	static final String ATTRIBUTE_POINT_TYPE = "type";
	static final String ATTRIBUTE_RESTORE_TYPE = "restoreType";
	static final String ATTRIBUTE_EDITABLE_FOLDER = "editable folder";

	protected static final String ACTION_REMOVE = "remove";

	protected ModbusLink link;
	protected Node node;

	public EditableFolder(ModbusLink link, Node node) {
		this.link = link;
		this.node = node;

		node.setAttribute(ATTRIBUTE_RESTORE_TYPE, new Value(ATTRIBUTE_EDITABLE_FOLDER));

		// create the minimum action list
		setEditAction();
		setRemoveAction();
		setAddPointAction();
	}

	protected void remove() {
		node.clearChildren();
		node.getParent().removeChild(node);
	}

	protected void rename(String newname) {
		duplicate(newname);
		remove();
	}

	protected abstract void edit(ActionResult event);

	protected abstract void addPoint(String name, PointType type, ActionResult event);

	protected void duplicate(String name) {
		JsonObject jsonObj = link.copySerializer.serialize();
		JsonObject parentObj = getParentJson(jsonObj);
		JsonObject nodeObj = parentObj.get(node.getName());
		parentObj.put(name, nodeObj);
		link.copyDeserializer.deserialize(jsonObj);

	};

	protected void addFolder(Node child) {
	};

	// action handlers for device folder
	protected class EditHandler implements Handler<ActionResult> {
		public void handle(ActionResult event) {
			edit(event);
		}
	}

	protected class RemoveHandler implements Handler<ActionResult> {
		public void handle(ActionResult event) {
			remove();
		}
	}

	protected class CopyHandler implements Handler<ActionResult> {
		public void handle(ActionResult event) {
			String newname = event.getParameter(ATTRIBUTE_NAME, ValueType.STRING).getString();
			if (newname.length() > 0 && !newname.equals(node.getName()))
				duplicate(newname);
		}
	}

	protected class AddFolderHandler implements Handler<ActionResult> {
		public void handle(ActionResult event) {
			String name = event.getParameter(ATTRIBUTE_NAME, ValueType.STRING).getString();
			Node child = node.createChild(name).build();
			addFolder(child);
		}
	}

	protected class AddPointHandler implements Handler<ActionResult> {
		public void handle(ActionResult event) {
			String name = event.getParameter(ATTRIBUTE_NAME, ValueType.STRING).getString();
			PointType type;

			try {
				type = PointType
						.valueOf(event.getParameter(ATTRIBUTE_POINT_TYPE, ValueType.STRING).getString().toUpperCase());
			} catch (Exception e) {
				LOGGER.error("invalid type");
				LOGGER.debug("error: ", e);
				return;
			}

			addPoint(name, type, event);
		}
	}

	public void setAddPointAction() {
	}

	public abstract void setEditAction();

	public void setRemoveAction() {
		Action act;
		act = new Action(Permission.READ, new RemoveHandler());
		node.createChild(ACTION_REMOVE).setAction(act).build().setSerializable(false);
	}

	void restoreLastSession() {
		if (node.getChildren() == null)
			return;

	}

	protected JsonObject getParentJson(JsonObject jobj) {
		return getParentJson(jobj, node);
	}

	private JsonObject getParentJson(JsonObject jobj, Node n) {
		return getParentJson(jobj, n.getParent()).get(n.getParent().getName());
	}

}
