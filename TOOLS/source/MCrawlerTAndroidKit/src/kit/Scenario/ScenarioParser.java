package kit.Scenario;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ScenarioParser {

	public static ScenarioData parse(File file) {
		/**
		 * s'assurer que le fichier est en utf-8
		 */

		try {
			InputStream inputStream = new FileInputStream(file);
			Reader reader = new InputStreamReader(inputStream, "UTF-8");
			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");

			SAXParserFactory fact = SAXParserFactory.newInstance();
			ScenarioData scen = new ScenarioData();

			SAXParser parser = fact.newSAXParser();
			DefaultHandler handler = new SgddHandler(file, scen);
			parser.parse(is, handler);// (file, handler);
			return scen;

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return null;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		} catch (SAXException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	public static class SgddHandler extends DefaultHandler {
		private final ScenarioData mScenario;
		private int mElementLevel = 0;
		private int mValidLevel = 0;

		private State mCurrentState = null;
		private Transition mCurrenTransition = null;
		private Action mCurrenAction = null;
		private Tree mCurrentTree = null;

		private final static int LEVEL_SCENARIO = 0;
		/**
		 * authors, alphabet,states,or transitions
		 * 
		 */
		private final static int LEVEL_INSIDE_SCENARIO = 1;

		/**
		 * state,action,transition
		 * 
		 */
		private final static int LEVEL_ELEMENT = 2;
		/**
		 * widgets or action
		 * 
		 */
		private final static int LEVEL_WIDGET_OR_ACTION = 3;
		private final static int LEVEL_WIDGET_TRANSITION = 4;

		public SgddHandler(File specAddXmlFile, ScenarioData scenData) {
			mScenario = scenData;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
		 */
		@Override
		public void endDocument() throws SAXException {
			super.endDocument();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
		 * java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {

			if (mValidLevel == mElementLevel) {
				mValidLevel--;
			}
			mElementLevel--;

			if (mValidLevel == mElementLevel) {

				switch (mValidLevel) {

				case LEVEL_ELEMENT:
					System.out.println("   insert "
							+ ((mCurrentState == null && mCurrenTransition != null) ? " transition :"
									+ mCurrenTransition.getId() : ((mCurrentState != null) ? mCurrentState
									.getId() + " " + mCurrentState.getName() : "not sate or transition")));
					if (Scenario.STATE.equals(qName)) {
						mScenario.setStates(mCurrentState, false, mCurrentState.isFinal());
						mCurrentState = null;
					}

					if (Scenario.TRANSITION.equals(qName)) {
						mScenario.setTransitions(mCurrenTransition, true);
						mCurrentTree.setTransitions(mCurrenTransition);
						mCurrenTransition = null;
					}
				case LEVEL_INSIDE_SCENARIO:
					if (Scenario.TRANSITIONS.equals(qName)) {
						mScenario.setTrees(mCurrentTree);
						mCurrentTree = null;
					}

				default:
					break;
				}

			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
		 */
		@Override
		public void startDocument() throws SAXException {
			// TODO Auto-generated method stub
			super.startDocument();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
		 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			if (mValidLevel == mElementLevel) {
				String value;
				switch (mValidLevel) {
				case LEVEL_SCENARIO:
					if (Scenario.SCENARIO.equals(qName)) {
						mScenario.setVersion(getAttributeValue(attributes, Scenario.VERSION));
						mValidLevel++;
					}
					break;
				case LEVEL_INSIDE_SCENARIO:
					/**
					 * handle alphabet
					 */
					if (Scenario.ALPHABET.equals(qName)) {
						mValidLevel++;
					}
					/**
					 * 
					 */
					/**
					 * handle states
					 */
					if (Scenario.STATES.equals(qName)) {
						value = getAttributeValue(attributes, Scenario.INITIAL);
						String time = getAttributeValue(attributes, Scenario.TIME);
						String type = getAttributeValue(attributes, Scenario.TYPE);
						String ue = getAttributeValue(attributes, Scenario.UE);
						State st = new State(value, getAttributeValue(attributes, Scenario.ID));
						st.setTime(time);
						st.setType(type);
						st.setUserEnvironment(ue);
						mScenario.setStates(st, true, false);
						mValidLevel++;
					}

					/**
					 * handle transitions
					 */
					if (Scenario.TRANSITIONS.equals(qName)) {
						value = getAttributeValue(attributes, Scenario.ID);
						mCurrentTree = new Tree("", value);
						mValidLevel++;
					}
					break;
				case LEVEL_ELEMENT:
					/**
					 * handle action
					 */
					if (Scenario.ACTION.equals(qName)) {
						mScenario.setActions(new Action(getAttributeValue(attributes, Scenario.NAME)));
						mValidLevel++;
					}

					/**
					 * handle state
					 */
					if (Scenario.STATE.equals(qName)) {
						State st = new State(getAttributeValue(attributes, Scenario.NAME), getAttributeValue(
								attributes, Scenario.ID));
						st.setTime(getAttributeValue(attributes, Scenario.TIME));
						st.setType(getAttributeValue(attributes, Scenario.TYPE));
						st.setUserEnvironment(getAttributeValue(attributes, Scenario.UE));
						st.setEndState(getAttributeValue(attributes, Scenario.END));
						mCurrentState = st;
						mValidLevel++;
					}

					/**
					 * handle transition
					 */
					if (Scenario.TRANSITION.equals(qName)) {
						/**
						 * chercher les etats source et dest
						 */
						String id = getAttributeValue(attributes, Scenario.ID);
						String destname = getAttributeValue(attributes, Scenario.DEST);
						State source = mScenario.getState(id, Scenario.SOURCE);
						State dest = mScenario.getState(id, Scenario.DEST);

						if (source != null && dest != null) {
							mCurrenTransition = new Transition(source, null, dest, id);
						} else {
							if (source != null && destname.equalsIgnoreCase(Scenario.END)) {
								State end = new State(Scenario.END, false, true, id);
								mCurrenTransition = new Transition(source, null, end, id);
							}
						}
						mValidLevel++;
					}
					break;
				case LEVEL_WIDGET_OR_ACTION:

					/**
					 * or action
					 */
					if (Scenario.WIDGET.equals(qName)) {
						// System.out.println("widget");
						Widget wig = (new Widget(getAttributeValue(attributes, Scenario.NAME),
								getAttributeValue(attributes, Scenario.TYPE), getAttributeValue(attributes,
										Scenario.POS_X), getAttributeValue(attributes, Scenario.POS_Y),
								getAttributeValue(attributes, Scenario.VISIBILITY), getAttributeValue(
										attributes, Scenario.ENABLE_STATUS), getAttributeValue(attributes,
										Scenario.PRESSES_STATUS), getAttributeValue(attributes,
										Scenario.SHOWN_STATUS), getAttributeValue(attributes,
										Scenario.SELECTION_STATUS), getAttributeValue(attributes,
										Scenario.VALUE), getAttributeValue(attributes,
										Scenario.ISLONGCLICKABLE), getAttributeValue(attributes,
										Scenario.ISCLICKABLE),
								getAttributeValue(attributes, Scenario.ISDIRTY), getAttributeValue(
										attributes, Scenario.ISFOCUSABLE), getAttributeValue(attributes,
										Scenario.ISFOCUS), getAttributeValue(attributes,
										Scenario.ISINEDITMODE), getAttributeValue(attributes,
										Scenario.ISVERTICALSCROLLBARENABLED)));
						/**
						 * add input type
						 */
						wig.setInputType(getAttributeValue(attributes, Scenario.INPUTTYPE));
				

						/**
						 * widget inside State
						 */
						if (mCurrentState != null) {
							mCurrentState.setWidget(wig);
						}
					}
					if (Scenario.ACTION.equals(qName)) {
						value = getAttributeValue(attributes, Scenario.NAME);
						mCurrenAction = new Action(value);
						mValidLevel++;
					}

					break;

				case LEVEL_WIDGET_TRANSITION:
					if (Scenario.WIDGET.equals(qName)) {
						Widget wig = null;
						if (getAttributeValue(attributes, Scenario.VALUE) != null) {
							wig = (new Widget(getAttributeValue(attributes, Scenario.NAME),
									getAttributeValue(attributes, Scenario.TYPE), "", "", "", "", "", "", "", getAttributeValue(
											attributes, Scenario.VALUE), "", "",
									"", "", "", "", ""));

						} else {
							wig = (new Widget(getAttributeValue(attributes, Scenario.NAME),
									getAttributeValue(attributes, Scenario.TYPE), getAttributeValue(
											attributes, Scenario.VALUE), getAttributeValue(attributes,
											Scenario.POS_X), getAttributeValue(attributes, Scenario.POS_Y),
									"", "", "", "", "", "", "", "", "", "", "", ""));

						}
						mCurrenAction.seWidget(wig);

						if (mCurrenTransition != null) {
							mCurrenTransition.setAction(mCurrenAction);
						}
					}

					if (Scenario.ERROR.equals(qName)) {
						String error = getAttributeValue(attributes, Scenario.NAME);
						mCurrenAction.setError(error);
						if (mCurrenTransition != null) {
							mCurrenTransition.setAction(mCurrenAction);
						}
					}

					break;

				}

			}
			mElementLevel++;
		}

		public ScenarioData getSpecAddData() {
			return mScenario;
		}

		private String getAttributeValue(Attributes attributes, String attributeName) {
			int count = attributes.getLength();
			for (int i = 0; i < count; i++) {
				String localName = attributes.getQName(i);
				int uri = attributes.getURI(i).length();
				if (attributeName.equals(localName) && uri == 0) {
					return attributes.getValue(i);
				}
			}

			return null;
		}
	}

}
