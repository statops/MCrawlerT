package kit.Scenario;

import java.util.ArrayList;

import javax.xml.transform.sax.TransformerHandler;

import kit.Config.Config;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class ScenarioXmlGeneratorHandler {

	protected TransformerHandler handler;
	private final static String CDATA = "CDATA";

	public ScenarioXmlGeneratorHandler(TransformerHandler handler) {
		this.handler = handler;
	}

	protected void addElement(String element, String name,
			ArrayList<ElementAttribute> attributes) throws SAXException {
		AttributesImpl ageAttrs = new AttributesImpl();
		for (ElementAttribute atr : attributes) {
			ageAttrs.addAttribute("", atr.getKey(), atr.getKey(), CDATA,
					atr.getValue());
		}
		handler.startElement("", element, element, ageAttrs);
		if (name != null) {
			handler.characters(name.toCharArray(), 0, name.length());
		}
		handler.endElement("", element, element);
	}

	private final static Attributes EMPTY_ATTRS = new AttributesImpl();

	public void generate(ScenarioData scenario) throws SAXException {
		handler.startDocument();

		/**
		 * root Element
		 */
		AttributesImpl rootAtrr = new AttributesImpl();
		String version = scenario.getVersion();
		rootAtrr.addAttribute("", Scenario.VERSION, Scenario.VERSION, CDATA,
				(version == null) ? "" : version);
		handler.startElement("", Scenario.SCENARIO, Scenario.SCENARIO, rootAtrr);

		/**
		 * alphabet elemnet
		 */
		handler.startElement("", Scenario.ALPHABET, Scenario.ALPHABET,
				EMPTY_ATTRS);
		for (Action action : scenario.getActions()) {
			addElement(Scenario.ACTION, null, new ElementAttribute(
					Scenario.NAME, action.getName()));

		}
		handler.endElement("", Scenario.ALPHABET, Scenario.ALPHABET);

		/**
		 * States elementt
		 */
		rootAtrr.clear();
		System.out.println("initial state name :"
				+ scenario.getInitialStateName());
		System.out.println("initial state :" + scenario.getInitialState());
		rootAtrr.addAttribute("", Scenario.INITIAL, Scenario.INITIAL, CDATA,
				scenario.getInitialStateName());
		String id = (scenario.getInitialState() != null) ? scenario
				.getInitialState().getId() : "0";
		rootAtrr.addAttribute("", Scenario.ID, Scenario.ID, CDATA, id);
		handler.startElement("", Scenario.STATES, Scenario.STATES, rootAtrr);
		// add state element for each state in ScenariData
		for (State state : scenario.getStates()) {
			/**
			 * la condition, � revoir ******
			 */
			if (state.getWidgets().isEmpty()
					&& state.getName().equalsIgnoreCase(
							scenario.getInitialStateName()))
				// do nothing;
				;
			else {
				if (Config.DEBUG) {
					// System.out.println("state to generate:");
					// System.out.println(state.toString());
				}

				addElement(state);
			}

		}

		handler.endElement("", Scenario.STATES, Scenario.STATES);

		/**
		 * Tree element
		 */
		for (Tree tree : scenario.getTrees()) {
			if (tree == null) {
				continue;
			}
			rootAtrr.clear();
			rootAtrr.addAttribute("", Scenario.ID, Scenario.ID, CDATA,
					tree.getId());
			rootAtrr.addAttribute("", Scenario.NAME, Scenario.NAME, CDATA,
					tree.getName());
			handler.startElement("", Scenario.TRANSITIONS,
					Scenario.TRANSITIONS, rootAtrr);
			for (Transition tr : tree.getTransitions()) {
				addElement(tr);
			}
			handler.endElement("", Scenario.TRANSITIONS, Scenario.TRANSITIONS);
		}
		/***
		 * Transitions Element
		 */

		/**
		 * end root
		 */
		handler.endElement("", Scenario.SCENARIO, Scenario.SCENARIO);
		handler.endDocument();
	}

	private void addElement(IScenarioElement element) throws SAXException {
		AttributesImpl eltAtrr = new AttributesImpl();
		if (element instanceof State) {
			State state = (State) element;
			// if (state.getType().equalsIgnoreCase("") &&
			// !state.getName().equalsIgnoreCase(Scenario.END)) {
			// return;
			// }
			String name = state.getName();
			String id = state.getId();
			String type = state.getType();
			String time = state.getTime();
			boolean ue = state.get_user_environment();
			boolean isFinal = state.isFinal();
			eltAtrr.addAttribute("", Scenario.NAME, Scenario.NAME, CDATA, name);
			eltAtrr.addAttribute("", Scenario.ID, Scenario.ID, CDATA, id);
			eltAtrr.addAttribute("", Scenario.TYPE, Scenario.TYPE, CDATA, type);
			eltAtrr.addAttribute("", Scenario.TIME, Scenario.TIME, CDATA, time);
			eltAtrr.addAttribute("", Scenario.END, Scenario.END, CDATA, ""
					+ isFinal);
			eltAtrr.addAttribute("", Scenario.UE, Scenario.UE, CDATA, "" + ue);
			handler.startElement("", Scenario.STATE, Scenario.STATE, eltAtrr);
			for (Widget wig : state.getWidgets()) {
				addWigElement(wig);
			}
			handler.endElement("", Scenario.STATE, Scenario.STATE);
			return;
		}

		if (element instanceof Transition) {
			Transition transition = (Transition) element;
			eltAtrr.addAttribute("", Scenario.ID, Scenario.ID, CDATA,
					transition.getId());
			eltAtrr.addAttribute("", Scenario.SOURCE, Scenario.SOURCE, CDATA,
					transition.getSource().getName());
			eltAtrr.addAttribute("", Scenario.DEST, Scenario.DEST, CDATA,
					transition.getDest().getName());
			handler.startElement("", Scenario.TRANSITION, Scenario.TRANSITION,
					eltAtrr);
			for (Action action : transition.getAction()) {
				addElement(action);
			}
			handler.endElement("", Scenario.TRANSITION, Scenario.TRANSITION);
			return;
		}

		if (element instanceof Action) {
			Action action = (Action) element;
			eltAtrr.addAttribute("", Scenario.NAME, Scenario.NAME, CDATA,
					action.getName());
			handler.startElement("", Scenario.ACTION, Scenario.ACTION, eltAtrr);
			if (Config.DEBUG) {
				System.out.println("Action : " + action.getName());
			}
			addWigElement(action);
			addErrorElement(action);
			handler.endElement("", Scenario.ACTION, Scenario.ACTION);
			return;
		}

	}

	/**
	 * @param action
	 * @throws SAXException
	 */
	private void addWigElement(Action action) throws SAXException {
		Widget wig = action.getWidget();
		addWigElement(wig);
	}

	/**
	 * @param wig
	 * @throws SAXException
	 */
	private void addWigElement(Widget wig) throws SAXException {
		ArrayList<ElementAttribute> atr = new ArrayList<ScenarioXmlGeneratorHandler.ElementAttribute>();
		if (wig == null) {
			return;
		}
		atr = getWidgetProprietyAttribute(wig, atr);
		if (wig.getValue() != null)
			atr.add(new ElementAttribute(Scenario.VALUE, wig.getValue()));
		addElement(Scenario.WIDGET, null, atr);
	}

	/**
	 * @param action
	 * @throws SAXException
	 */
	private void addErrorElement(Action action) throws SAXException {
		String error = action.getError();
		if (error != null) {
			ArrayList<ElementAttribute> Erroratr = new ArrayList<ScenarioXmlGeneratorHandler.ElementAttribute>();
			Erroratr.add(new ElementAttribute(Scenario.NAME, error));
			addElement(Scenario.ERROR, null, Erroratr);
		}

	}

	private ArrayList<ElementAttribute> getWidgetProprietyAttribute(Widget wig,
			ArrayList<ElementAttribute> atr) {
		if (wig == null) {
			if (Config.DEBUG) {
				System.out.println("Wig is null");
			}
		}
		atr.add(new ElementAttribute(Scenario.NAME, wig.getName()));
		atr.add(new ElementAttribute(Scenario.TYPE, wig.getType()));
		atr.add(new ElementAttribute(Scenario.POS_X, wig.getPosX()));
		atr.add(new ElementAttribute(Scenario.POS_Y, wig.getPosY()));
		atr.add(new ElementAttribute(Scenario.VISIBILITY, wig.getVisibility()));
		atr.add(new ElementAttribute(Scenario.ENABLE_STATUS, wig
				.getSatusEnable()));
		atr.add(new ElementAttribute(Scenario.PRESSES_STATUS, wig
				.getStatusPressed()));
		atr.add(new ElementAttribute(Scenario.SHOWN_STATUS, wig
				.getStatusShown()));
		atr.add(new ElementAttribute(Scenario.SELECTION_STATUS, wig
				.getStatusSelection()));
		atr.add(new ElementAttribute(Scenario.VALUE, (wig.getValue())));
		atr.add(new ElementAttribute(Scenario.ISLONGCLICKABLE, wig
				.getIsLongClickable()));
		atr.add(new ElementAttribute(Scenario.ISCLICKABLE, wig.getIsClickable()));
		atr.add(new ElementAttribute(Scenario.ISDIRTY, wig.getIsDirty()));
		atr.add(new ElementAttribute(Scenario.ISFOCUSABLE, wig.getIsFocusable()));
		atr.add(new ElementAttribute(Scenario.ISFOCUS, wig.getIsFocus()));
		atr.add(new ElementAttribute(Scenario.ISINEDITMODE, wig
				.getIsInEditMode()));
		atr.add(new ElementAttribute(Scenario.ISVERTICALSCROLLBARENABLED, wig
				.getIsVerticalScrollBarEnabled()));
		atr.add(new ElementAttribute(Scenario.INPUTTYPE, wig.getInputType()));
		return atr;

	}

	private void addElement(String element, String name, ElementAttribute atr)
			throws SAXException {
		AttributesImpl ageAttrs = new AttributesImpl();
		ageAttrs.addAttribute("", atr.getKey(), atr.getKey(), CDATA,
				atr.getValue());
		handler.startElement("", element, element, ageAttrs);
		if (name != null) {
			handler.characters(name.toCharArray(), 0, name.length());
		}
		handler.endElement("", element, element);
	}

	public class ElementAttribute {
		private final String key;
		private final String value;

		public ElementAttribute(String name, String val) {
			key = name;
			value = val;
		}

		/**
		 * @return the QName (key)
		 */
		public String getKey() {
			return key;
		}

		/**
		 * @return the value of the attributes
		 */
		public String getValue() {
			return value;
		}

	}
}
