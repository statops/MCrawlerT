package jp.gr.java_conf.hatalab.mnv;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;
import android.widget.ScrollView;

public class ScrollWrappableEditText extends EditText {
	
	
	private static final String LOGGING_TAG = ScrollWrappableEditText.class.getSimpleName();


	// Context menu entries
	private static final int ID_SELECT_ALL = android.R.id.selectAll;
	private static final int ID_START_SELECTING_TEXT = android.R.id.startSelectingText;
	private static final int ID_STOP_SELECTING_TEXT = android.R.id.stopSelectingText;
	private static final int ID_CUT = android.R.id.cut;
	private static final int ID_COPY = android.R.id.copy;
	private static final int ID_PASTE = android.R.id.paste;
	private static final int ID_COPY_URL = android.R.id.copyUrl;
	private static final int ID_SWITCH_INPUT_METHOD = android.R.id.switchInputMethod;
	private static final int ID_ADD_TO_DICTIONARY = android.R.id.addToDictionary;

	private boolean startSelectingFlag = false;
	
	public ScrollWrappableEditText(Context context) {
		super(context);
	}
    public ScrollWrappableEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ScrollWrappableEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    
    
    @Override
    public boolean onTextContextMenuItem(int id) {
    	switch (id) {
    	case ID_START_SELECTING_TEXT:
    		startSelectingFlag = true;
    		break;
    		
    	case ID_STOP_SELECTING_TEXT:
    	case ID_SELECT_ALL:
    	case ID_CUT:                
    	case ID_COPY:
    	case ID_PASTE:
    	case ID_COPY_URL:
    	case ID_SWITCH_INPUT_METHOD:
    	case ID_ADD_TO_DICTIONARY:
    	default:
    		startSelectingFlag = false;

    		break;
    	}
    	
    	
    	return super.onTextContextMenuItem(id);
    }    
    
    public boolean getSelectingFlag(){
    	return startSelectingFlag;
    }
    /**
     * Don't request this view move if it is wrapped in a ScrollView
     * @see android.view.View#requestRectangleOnScreen(android.graphics.Rect, boolean)
     */
    /**
    @Override
    public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) {
        // Always return true, the ScrollView around this will handle the proper rectangle being on screen
        if (getParent() instanceof ScrollView) {
//            Log.d(LOGGING_TAG, "Wrapped in ScrollView tell call request is complete");
            return true;
        } else {
//            Log.d(LOGGING_TAG, "Not wrapped by ScrollView, handle normally");
            return super.requestRectangleOnScreen(rectangle, immediate);
        }
    }
    **/
    
    
}
