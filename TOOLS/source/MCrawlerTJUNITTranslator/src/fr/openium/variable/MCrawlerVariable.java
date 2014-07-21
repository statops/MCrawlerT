/**
 * 
 */
package fr.openium.variable;

/**
 * @author Stassia
 * 
 */
public class MCrawlerVariable {
	/**
	 * @author Stassia
	 * 
	 */
	public class Guard {
		// TODO
	}

	/**
	 * @author Stassia
	 * 
	 */
	public class Action extends kit.Scenario.Action {

		/**
		 * @param name
		 * @param wig
		 */
		public Action(String name, kit.Scenario.Widget wig) {
			super(name, wig);
		}

		public Action(String name) {
			super(name);
		}
	}

	private static final String TAG = MCrawlerVariable.class.getName();

	public class Widget extends kit.Scenario.Widget {

		/**
		 * @param name
		 * @param type
		 * @param posX
		 * @param posY
		 * @param visibility
		 * @param statusEnable
		 * @param statusPressed
		 * @param statusShown
		 * @param statusSelection
		 * @param value
		 */
		public Widget(String name, String type, String posX, String posY, String visibility,
				String statusEnable, String statusPressed, String statusShown, String statusSelection,
				String value) {
			super(name, type, posX, posY, visibility, statusEnable, statusPressed, statusShown,
					statusSelection, value, "", "", "", "", "", "", "");
		}

		/**
		 * Widgets properties
		 * 
		 * @param name
		 * @param type
		 * @param posX
		 * @param posY
		 * @param visibility
		 * @param statusEnable
		 * @param statusPressed
		 * @param statusShown
		 * @param statusSelection
		 * @param value
		 * @param LongClickable
		 * @param Clickable
		 * @param Dirty
		 * @param Focusable
		 * @param Focus
		 * @param InEditMode
		 * @param VerticalScrollBarEnabled
		 */

		public Widget(kit.Scenario.Widget wig) {
			super(wig.getName(), wig.getType(), wig.getPosX(), wig.getPosY(), wig.getVisibility(), wig
					.getSatusEnable(), wig.getStatusPressed(), wig.getStatusShown(),
					wig.getStatusSelection(), wig.getValue(), wig.getIsLongClickable(), wig.getIsClickable(),
					wig.getIsDirty(), wig.getIsFocusable(), wig.getIsFocus(), wig.getIsInEditMode(), wig
							.getIsVerticalScrollBarEnabled());
		}

	}

	public class State extends kit.Scenario.State {

		/**
		 * @param name
		 * @param initStatus
		 * @param finalstatus
		 * @param id
		 */

		public State(String name, boolean initStatus, boolean finalstatus, String id, Widget... wigs) {
			super(name, initStatus, finalstatus, id);
			setWidgets(wigs);
		}

		/**
		 * @param wigs
		 */
		private void setWidgets(Widget[] wigs) {
			for (Widget wig : wigs) {
				setWidget(wig);
			}

		}

		public State(kit.Scenario.State state) {
			super(state.getName(), state.isInit(), state.isFinal(), state.getId());	
			for (kit.Scenario.Widget wig : state.getWidgets()) {
				setWidget(wig);
			}
			
		}

	}

}
