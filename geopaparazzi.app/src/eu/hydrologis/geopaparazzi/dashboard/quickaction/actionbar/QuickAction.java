package eu.hydrologis.geopaparazzi.dashboard.quickaction.actionbar;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

import eu.hydrologis.geopaparazzi.R;

/**
 * Taken from http://www.londatiga.net/it/how-to-create-quickaction-dialog-in-android/
 * 
 * Popup window, shows action list as icon and text like the one in Gallery3D app. 
 * 
 * @author Lorensius. W. T
 */
public class QuickAction extends CustomPopupWindow {
    private final View root;
    private final LayoutInflater inflater;
    private final Context context;

    /**
     * 
     */
    public static final int ANIM_GROW_FROM_LEFT = 1;
    /**
     * 
     */
    public static final int ANIM_GROW_FROM_RIGHT = 2;
    /**
     * 
     */
    public static final int ANIM_GROW_FROM_CENTER = 3;
    /**
     * 
     */
    public static final int ANIM_REFLECT = 4;
    /**
     * 
     */
    public static final int ANIM_AUTO = 5;

    private int animStyle;
    private ViewGroup mTrack;
    private ScrollView scroller;
    private ArrayList<ActionItem> actionList;

    /**
     * Constructor
     * 
     * @param anchor {@link View} on where the popup window should be displayed
     */
    public QuickAction( View anchor ) {
        super(anchor);

        actionList = new ArrayList<ActionItem>();
        context = anchor.getContext();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        root = (ViewGroup) inflater.inflate(R.layout.popup, null);

        setContentView(root);

        mTrack = (ViewGroup) root.findViewById(R.id.tracks);
        scroller = (ScrollView) root.findViewById(R.id.scroller);
        animStyle = ANIM_AUTO;
    }

    /**
     * Set animation style
     * 
     * @param animStyle animation style, default is set to ANIM_AUTO
     */
    public void setAnimStyle( int animStyle ) {
        this.animStyle = animStyle;
    }

    /**
     * Add action item
     * 
     * @param action  {@link ActionItem} object
     */
    public void addActionItem( ActionItem action ) {
        actionList.add(action);
    }

    /**
     * Show popup window. Popup is automatically positioned, on top or bottom of anchor view.
     * 
     */
    public void show() {
        preShow();

        int xPos, yPos;

        int[] location = new int[2];

        anchor.getLocationOnScreen(location);

        Rect anchorRect = new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1] + anchor.getHeight());

        createActionList();

        root.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        int rootHeight = root.getMeasuredHeight();
        int rootWidth = root.getMeasuredWidth();

        int screenWidth = windowManager.getDefaultDisplay().getWidth();
        int screenHeight = windowManager.getDefaultDisplay().getHeight();

        // automatically get X coord of popup (top left)
        if ((anchorRect.left + rootWidth) > screenWidth) {
            xPos = anchorRect.left - (rootWidth - anchor.getWidth());
        } else {
            if (anchor.getWidth() > rootWidth) {
                xPos = anchorRect.centerX() - (rootWidth / 2);
            } else {
                xPos = anchorRect.left;
            }
        }

        int dyTop = anchorRect.top;
        int dyBottom = screenHeight - anchorRect.bottom;

        boolean onTop = (dyTop > dyBottom) ? true : false;

        if (onTop) {
            if (rootHeight > dyTop) {
                yPos = 15;
                LayoutParams l = scroller.getLayoutParams();
                l.height = dyTop - anchor.getHeight();
            } else {
                yPos = anchorRect.top - rootHeight;
            }
        } else {
            yPos = anchorRect.bottom;

            if (rootHeight > dyBottom) {
                LayoutParams l = scroller.getLayoutParams();
                l.height = dyBottom;
            }
        }

        setAnimationStyle(screenWidth, anchorRect.centerX(), onTop);

        window.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
    }

    /**
     * Set animation style
     * 
     * @param screenWidth screen width
     * @param requestedX distance from left edge
     * @param onTop flag to indicate where the popup should be displayed. Set TRUE if displayed on top of anchor view
     * 		  and vice versa
     */
    private void setAnimationStyle( int screenWidth, int requestedX, boolean onTop ) {

        switch( animStyle ) {
        case ANIM_GROW_FROM_LEFT:
            window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left : R.style.Animations_PopDownMenu_Left);
            break;

        case ANIM_GROW_FROM_RIGHT:
            window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right : R.style.Animations_PopDownMenu_Right);
            break;

        case ANIM_GROW_FROM_CENTER:
            window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center : R.style.Animations_PopDownMenu_Center);
            break;

        case ANIM_REFLECT:
            window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Reflect : R.style.Animations_PopDownMenu_Reflect);
            break;

        case ANIM_AUTO:
            window.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right : R.style.Animations_PopDownMenu_Right);
            break;
        }
    }

    /**
     * Create action list
     */
    private void createActionList() {
        View view = null;
        String title;
        Drawable icon;
        OnClickListener listener;

        for( int i = 0; i < actionList.size(); i++ ) {
            title = actionList.get(i).getTitle();
            icon = actionList.get(i).getIcon();
            listener = actionList.get(i).getListener();

            view = getActionItem(title, icon, listener);

            view.setFocusable(true);
            view.setClickable(true);

            mTrack.addView(view);
        }
    }

    /**
     * Get action item {@link View}
     * 
     * @param title action item title
     * @param icon {@link Drawable} action item icon
     * @param listener {@link View.OnClickListener} action item listener
     * @return action item {@link View}
     */
    private View getActionItem( String title, Drawable icon, OnClickListener listener ) {
        LinearLayout container = (LinearLayout) inflater.inflate(R.layout.quickaction_item2, null);

        ImageView img = (ImageView) container.findViewById(R.id.icon);
        TextView text = (TextView) container.findViewById(R.id.title);

        if (icon != null) {
            img.setImageDrawable(icon);
        }

        if (title != null) {
            text.setText(title);
        }

        if (listener != null) {
            container.setOnClickListener(listener);
        }

        return container;
    }

}