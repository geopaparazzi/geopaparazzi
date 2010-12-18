/**
 * 
 */
package eu.hydrologis.geopaparazzi.dashboard;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;

/**
 * @author ET
 * 
 */
public class DashboardItem extends ViewGroup {
	private int titleId;
	private int imageId;
	private String onClickMethod;

	/**
	 * @param context
	 * @param attrs
	 */
	public DashboardItem(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray styledAttributes = context.obtainStyledAttributes(attrs,
				R.styleable.DashboardItem);

		titleId = styledAttributes.getResourceId(
				R.styleable.DashboardItem_title, -1);
		// if (titleId == -1)
		// titleId = "no title";
		imageId = styledAttributes.getResourceId(
				R.styleable.DashboardItem_image, -1);
		onClickMethod = styledAttributes
				.getString(R.styleable.DashboardItem_onClick);

		styledAttributes.recycle();

		View view = LayoutInflater.from(context).inflate(
				R.layout.dashboard_button_layout, this); // in this case the
															// class itself is
															// the parent

	}

	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
		int availableHeight = MeasureSpec.getSize(heightMeasureSpec);

		// bisogna passare le info dello spazio ad eventuali figli
		int childWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth,
				MeasureSpec.AT_MOST);
		int childHeightSpec = MeasureSpec.makeMeasureSpec(availableHeight,
				MeasureSpec.AT_MOST);
		View child = getChildAt(0);
		child.measure(childWidthSpec, childHeightSpec);

		setMeasuredDimension(availableWidth, availableHeight);
		// setMeasuredDimension(
		// resolveSize(child.getMeasuredWidth(), widthMeasureSpec),
		// resolveSize(child.getMeasuredHeight(), heightMeasureSpec));

	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {

		View child = getChildAt(0);

		// valori dinamici
		TextView labelView = (TextView) child
				.findViewById(R.id.dashboard_item_text);
		labelView.setText(titleId);

		ImageButton imageButton = (ImageButton) child
				.findViewById(R.id.dashboard_item_image);
		imageButton.setImageResource(imageId);

		if (onClickMethod != null) {
			imageButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					// gestione click
					Context context = getContext();
					try {
						Method ocMethod = context.getClass().getMethod(
								onClickMethod, int.class);
						ocMethod.invoke(context, getId());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}

		// int measuredWidth = child.getMeasuredWidth();
		// int measuredHeight = child.getMeasuredHeight();
		// int posX = (right - left - measuredWidth) / 2;
		// int posY = (bottom - top - measuredHeight) / 2;
		//
		// child.layout(posX, posY, measuredWidth, measuredHeight);
		child.layout(0, 0, right - left, bottom - top);

	}

	public class LayoutParams extends ViewGroup.LayoutParams {

		private int titleId;
		private int imageId;
		private String onClickMethod;

		public LayoutParams(Context context, AttributeSet atts) {
			super(context, atts);

			TypedArray styledAttributes = context.obtainStyledAttributes(atts,
					R.styleable.DashboardItem);

			titleId = styledAttributes.getResourceId(
					R.styleable.DashboardItem_title, -1);
			// if (titleId == -1)
			// titleId = "no title";
			imageId = styledAttributes.getResourceId(
					R.styleable.DashboardItem_image, -1);
			onClickMethod = styledAttributes
					.getString(R.styleable.DashboardItem_onClick);

			styledAttributes.recycle();
		}

	}
}
